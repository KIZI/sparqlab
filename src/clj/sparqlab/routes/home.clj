(ns sparqlab.routes.home
  (:require [sparqlab.layout :as layout]
            [sparqlab.sparql :as sparql]
            [sparqlab.store :refer [construct-query select-query]]
            [sparqlab.prefixes :as prefix]
            [sparqlab.util :refer [kahn-sort query-file?]]
            [sparqlab.exercise :as exercise]
            [sparqlab.config :refer [local-language]]
            [compojure.core :refer [context defroutes GET POST]]
            [clojure.tools.logging :as log]
            [ring.util.http-response :as response]
            [markdown.core :refer [md-to-html-string]]
            [selmer.filters :refer [add-filter!]]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:import [java.io PushbackReader]))

; Selmer template filters
(add-filter! :markdown (fn [s] [:safe (md-to-html-string s)]))

(add-filter! :dec dec)

(def a-year
  "One year in seconds"
  (* 60 60 24 365))

(def cookie-ns
  "sparqlab-exercise-")

(defn set-cookie
  [response k value & {:as data}]
  (assoc-in response
            [:cookies k]
            (merge {:max-age a-year
                    :path "/"}
                   data
                   {:value value})))

(defn mark-exercise-as-done
  [response id]
  (set-cookie response (str cookie-ns id) true))

(defn mark-exercises-as-done
  [exercises exercises-done]
  (map (fn [{:keys [id]
             :as exercise}]
         (if (exercises-done id)
           (assoc exercise :done true)
           exercise))
       exercises))

(defn get-exercise
  [id]
  (let [model (-> "get_exercise"
                  (sparql/sparql-template {:exercise (prefix/exercise id)
                                           :language local-language})
                  construct-query)
        select-fn (comp (partial sparql/select-query model) sparql/sparql-template)
        description (first (select-fn "get_exercise_description"))
        prohibits (select-fn "get_prohibits")
        requires (select-fn "get_requires")
        construct-template (and (:reveal description)
                                (sparql/get-construct-template (:query description)))]
    (assoc description
           :prohibits prohibits
           :requires requires
           :construct-template construct-template)))

(defn get-prerequisites
  [id]
  (select-query (sparql/sparql-template "get_prerequisites" {:exercise (prefix/exercise id)})))

(defn get-exercises
  []
  (->> "get_exercises"
       sparql/sparql-template
       select-query))

(def spin-dependencies
  "Dependencies between SPIN SPARQL language constructs"
  (let [deps (-> "spin_dependencies.edn" io/resource io/reader PushbackReader. edn/read)]
    (into {} (map (fn [[k v]] [(prefix/sp k) (into #{} (map prefix/sp v))]) deps))))

(defn sort-exercises-by-dependencies
  "Sort exercises by the SPARQL language constructs they depend on by using them."
  []
  (letfn [(spin-term? [term] (string/starts-with? term (prefix/sp)))
          (group-exercises-by-constructs [acc {:keys [exercise construct]}]
            (if (contains? acc construct)
              (update acc construct conj exercise)
              (assoc acc construct #{exercise})))]
    (->> "extract_exercise_constructs"
         sparql/sparql-template 
         select-query
         (reduce group-exercises-by-constructs {}) 
         (merge spin-dependencies)
         kahn-sort
         (remove spin-term?))))

(defn get-exercises-done
  [request]
  (->> (:cookies request)
       (filter (every-pred (comp #(string/starts-with? % cookie-ns) key)
                           (comp (partial = "true") :value val)))
       (map (comp #(subs % (count cookie-ns)) key))
       (into #{})))

(defn get-namespace-prefixes
  []
  (->> "get_namespace_prefixes"
       sparql/sparql-template
       select-query
       (map (fn [{{prefix "@value"} :prefix
                  {nspace "@id"} :namespace}]
              {:prefix prefix
               :namespace nspace}))))

(defn home-page
  [request]
  (let [exercises-done (get-exercises-done request)]
    (layout/render "home.html" {:exercises (mark-exercises-as-done (get-exercises) exercises-done)})))

(defn format-invalid-query
  "Render invalid query using syntax validation result."
  [{:keys [expected message offset query]}]
  (if message
    {:message message
     :query query}
    {:head (subs query 0 offset)
     :error "   " ; Error placeholder
     :tail (subs query offset)
     :expected (string/join \newline expected)}))

(defn evaluate-exercise
  [{{query "query"} :form-params}
   id]
  (let [{:keys [valid?] :as validation-result} (sparql/valid-query? query)]
    (if valid?
      (let [{canonical-query :query
             :keys [prohibits requires]
             :as exercise} (get-exercise id)
            verdict (exercise/evaluate-exercise canonical-query
                                                query
                                                :prohibited (map :prohibited prohibits)
                                                :required (map :required requires))]
        (cond-> (layout/render "evaluation.html" (merge exercise verdict))
          (:equal? verdict) (mark-exercise-as-done id)))
      (layout/render "sparql_syntax_error.html" (format-invalid-query validation-result)))))

(defn search-exercises
  [search-term search-constructs]
  (select-query (sparql/sparql-template "find_exercises" {:search-term search-term
                                                          :search-constructs search-constructs})))

(defn show-exercise
  [id]
  (let [exercise (get-exercise id)
        prerequisites (get-prerequisites id)]
    (layout/render "exercise.html" (assoc exercise
                                          :id id
                                          :prerequisites prerequisites))))

(defn sparql-endpoint
  []
  (layout/render "endpoint.html"))

(defn about-page
  []
  (let [sparql-constructs (select-query (sparql/sparql-template "get_sparql_constructs"
                                                                {:language local-language}))]
    (layout/render "about.html" {:sparql-constructs sparql-constructs})))

(defn pad-prefixes
  "Pad `prefixes` by prepending spaces to have the same width."
  [prefixes]
  (let [longest-prefix-length (->> prefixes
                                   (map (comp count :prefix))
                                   (sort #(compare %2 %1))
                                   first)
        get-padding (fn [prefix]
                      (apply str (repeat (inc (- longest-prefix-length (count prefix))) " ")))
        pad-prefix (fn [{:keys [prefix] :as m}]
                     (assoc m :padding (get-padding prefix)))]
    (map pad-prefix prefixes)))

(defn data-page
  []
  (let [prefixes (get-namespace-prefixes)
        padded-prefixes (pad-prefixes prefixes)]
    (layout/render "data.html" {:prefixes padded-prefixes})))

(defn search-results
  [search-term search-constructs]
  (let [exercises-found (search-exercises search-term search-constructs)]
    (layout/render "search_results.html" {:search-term search-term
                                          :search-constructs (exercise/get-construct-labels search-constructs
                                                                                            local-language)
                                          :exercises exercises-found})))

(defroutes home-routes
  (GET "/" request (home-page request))
  (context "/exercise" []
           (GET "/show/:id" [id] (show-exercise id))
           (POST "/evaluate/:id" [id :as request] (evaluate-exercise request id)))
  (GET "/endpoint" [] (sparql-endpoint))
  (GET "/data" [] (data-page))
  (GET "/about" [] (about-page))
  (GET "/search"
       {{search-term :q search-constructs :construct} :params}
       (search-results search-term search-constructs)))
