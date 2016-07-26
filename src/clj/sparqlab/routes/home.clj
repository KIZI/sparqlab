(ns sparqlab.routes.home
  (:require [sparqlab.layout :as layout]
            [sparqlab.sparql :as sparql]
            [sparqlab.store :refer [select-query]]
            [sparqlab.prefixes :as prefix]
            [sparqlab.util :refer [query-file?]]
            [compojure.core :refer [context defroutes GET POST]]
            [clojure.tools.logging :as log]
            [ring.util.http-response :as response]
            [markdown.core :refer [md-to-html-string]]
            [selmer.filters :refer [add-filter!]]
            [clojure.string :as string]))

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
  (set-cookie response
              (str cookie-ns id)
              true))

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
  (-> "get_exercise"
      (sparql/sparql-template {:exercise (prefix/exercise id)})
      select-query
      first
      sparql/->plain-literals))

(defn get-prerequisites
  [id]
  (->> (sparql/sparql-template "get_prerequisites" {:exercise (prefix/exercise id)})
       select-query
       (map sparql/->plain-literals)))

(defn get-exercises
  []
  (->> "get_exercises"
       sparql/sparql-template
       select-query 
       (map sparql/->plain-literals)))

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

(defn evaluate-exercise
  [{{query "query"} :form-params}
   id]
  (let [{canonical-query :query
         :as exercise} (get-exercise id)
        verdict (sparql/evaluate-exercise canonical-query query)]
    (cond-> (layout/render "evaluation.html" (merge exercise verdict))
      (:equal? verdict) (mark-exercise-as-done id))))

(defn search-exercises
  [search-term]
  (->> (sparql/sparql-template "find_exercises" {:search-term search-term})
       select-query
       (map sparql/->plain-literals)))

(defn show-exercise
  [id]
  (let [exercise (get-exercise id)
        prerequisites (get-prerequisites id)]
    (layout/render "exercise.html" (assoc exercise :prerequisites prerequisites))))

(defn sparql-endpoint
  []
  (layout/render "endpoint.html"))

(defn about-page
  []
  (layout/render "about.html"))

(defn data-page
  []
  (let [prefixes (get-namespace-prefixes)
        longest-prefix-length (->> prefixes
                                   (map (comp count :prefix))
                                   (sort #(compare %2 %1))
                                   first)
        padded-prefixes (map (fn [{:keys [prefix]
                                   :as m}]
                               (assoc m :padding (apply str (repeat (inc (- longest-prefix-length
                                                                            (count prefix)))
                                                                    " "))))
                             prefixes)]
    (layout/render "data.html" {:prefixes padded-prefixes})))

(defn search-results
  [search-term]
  (let [exercises-found (search-exercises search-term)]
    (layout/render "search_results.html" {:search-term search-term
                                          :exercises exercises-found})))

(defroutes home-routes
  (GET "/" request (home-page request))
  (context "/exercise" []
           (GET "/show/:id" [id] (show-exercise id))
           (POST "/evaluate/:id" [id :as request] (evaluate-exercise request id)))
  (GET "/endpoint" [] (sparql-endpoint))
  (GET "/data" [] (data-page))
  (GET "/about" [] (about-page))
  (GET "/search" {{search-term :q} :params} (search-results search-term)))
