(ns sparqlab.routes.home
  (:require [sparqlab.layout :as layout]
            [sparqlab.sparql :as sparql]
            [sparqlab.store :refer [construct-query select-query]]
            [sparqlab.prefixes :as prefix]
            [sparqlab.util :refer [kahn-sort query-file?]]
            [sparqlab.exercise :as exercise]
            [sparqlab.config :refer [local-language]]
            [sparqlab.cookies :as cookie]
            [compojure.core :refer [context defroutes GET POST]]
            [clojure.tools.logging :as log]
            [markdown.core :refer [md-to-html-string]]
            [selmer.filters :refer [add-filter!]]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:import (java.io PushbackReader)))

; Selmer template filters
(add-filter! :markdown (fn [s] [:safe (md-to-html-string s)]))

(add-filter! :dec dec)

(defn group-values-by-key
  "Group values at `value-key` in collection `coll` by `group-key`."
  [group-key value-key coll]
  (letfn [(group-fn [acc item]
            (let [k (group-key item)
                  v (value-key item)]
              (if (contains? acc k)
                (update acc k conj v)
                (assoc acc k #{v}))))]
    (reduce group-fn {} coll)))

(defn mark-exercises-with-statuses
  [exercises exercise-statuses]
  (map (fn [{:keys [id]
             :as exercise}]
         (if-let [exercise-status (exercise-statuses id)]
           (assoc exercise :status exercise-status)
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

(defn get-exercises-by-difficulty
  [exercise-statuses]
  (let [exercises (-> "get_exercises_by_difficulty"
                      (sparql/sparql-template {:language local-language})
                      select-query
                      (mark-exercises-with-statuses exercise-statuses))]
    (group-by :difficultyLevel exercises)))

(def spin-dependencies
  "Dependencies between SPIN SPARQL language constructs"
  (let [deps (-> "spin_dependencies.edn" io/resource io/reader PushbackReader. edn/read)]
    (into {} (map (fn [[k v]] [(prefix/sp k) (into #{} (map prefix/sp v))]) deps))))

(defn sort-exercises-by-dependencies
  "Sort exercises by the SPARQL language constructs they depend on by using them."
  []
  (letfn [(spin-term? [term] (string/starts-with? term (prefix/sp)))]
    (->> "extract_exercise_constructs"
         sparql/sparql-template 
         select-query
         (group-values-by-key :construct :exercise)
         (merge spin-dependencies)
         kahn-sort
         (remove spin-term?))))

(defn get-namespace-prefixes
  []
  (select-query (sparql/sparql-template "get_namespace_prefixes")))

(defn exercises-by-difficulty
  [{tr :tempura/tr
    :as request}]
  (let [exercise-statuses (cookie/get-exercise-statuses request)
        {easy 0
         normal 1
         hard 2} (get-exercises-by-difficulty exercise-statuses)]
    (layout/render "exercises_by_difficulty.html" {:title (tr [:exercises-by-difficulty/title])
                                                   :easy easy
                                                   :normal normal
                                                   :hard hard})))

(defn get-exercises-by-categories
  [exercise-statuses]
  (let [exercises (-> "get_exercises_by_category"
                      (sparql/sparql-template {:language local-language})
                      select-query
                      (mark-exercises-with-statuses exercise-statuses))]
    (sort-by (comp string/lower-case key)
             (group-by :categoryLabel exercises))))

(defn exercises-by-categories
  [{tr :tempura/tr
    :as request}]
  (let [exercise-statuses (cookie/get-exercise-statuses request)
        exercises (get-exercises-by-categories exercise-statuses)]
    (layout/render "exercises_by_category.html" {:exercises exercises
                                                 :title (tr [:exercises-by-category/title])})))

(defn get-exercises-by-language-constructs
  [exercise-statuses]
  (let [sorted-exercises (sort-exercises-by-dependencies)
        exercises (-> "get_exercises_by_difficulty"
                      (sparql/sparql-template {:language local-language})
                      select-query
                      (mark-exercises-with-statuses exercise-statuses))]
    (sort-by (comp #(.indexOf sorted-exercises %) prefix/exercise :id) exercises)))

(defn exercises-by-language-constructs
  [{tr :tempura/tr
    :as request}]
  (let [exercise-statuses (cookie/get-exercise-statuses request)
        exercises (get-exercises-by-language-constructs exercise-statuses)]
    (layout/render "exercises_by_language_constructs.html"
                   {:title (tr [:exercises-by-language-constructs/title])
                    :exercises exercises})))

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
  [{{query "query"} :form-params
    tr :tempura/tr
    :as request}
   id]
  (let [{:keys [valid?] :as validation-result} (sparql/valid-query? query)]
    (if valid?
      (let [{canonical-query :query
             :keys [prohibits requires]
             :as exercise} (get-exercise id)
            verdict (exercise/evaluate-exercise canonical-query
                                                query
                                                :prohibited (map :prohibited prohibits)
                                                :required (map :required requires))
            exercise-status (get (cookie/get-exercise-statuses request) id)]
        (cond-> (layout/render "evaluation.html"
                               (assoc (merge exercise verdict)
                                      :title (tr [:evaluation/title] [(:name exercise)])))
          (and (:equal? verdict) (not= exercise-status "revealed")) (cookie/mark-exercise-as-solved id)))
      (layout/render "sparql_syntax_error.html"
                     (assoc (format-invalid-query validation-result)
                            :title (tr [:sparql-syntax-error/title]))))))

(defn search-exercises
  "Search exercises for a `search-term` or several SPARQL `search-constructs`."
  [request search-term search-constructs]
  (let [exercise-statuses (cookie/get-exercise-statuses request)
        exercises-found (->> {:language local-language
                              :search-term search-term
                              :search-constructs search-constructs}
                             (sparql/sparql-template "find_exercises")
                             select-query)]
    (mark-exercises-with-statuses exercises-found exercise-statuses)))

(defn show-exercise
  [id]
  (let [exercise (get-exercise id)
        prerequisites (get-prerequisites id)]
    (layout/render "exercise.html" (assoc exercise
                                          :id id
                                          :prerequisites prerequisites
                                          :title (:name exercise)))))

(defn sparql-endpoint
  [{tr :tempura/tr}]
  (layout/render "endpoint.html" {:title (tr [:endpoint/title])}))

(defn about-page
  [{tr :tempura/tr}]
  (let [sparql-constructs (select-query (sparql/sparql-template "get_sparql_constructs"
                                                                {:language local-language}))]
    (layout/render "about.html" {:sparql-constructs sparql-constructs
                                 :title (tr [:about/title])})))

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
  [{tr :tempura/tr}]
  (let [prefixes (get-namespace-prefixes)
        padded-prefixes (pad-prefixes prefixes)]
    (layout/render "data.html" {:prefixes padded-prefixes
                                :title (tr [:data/title])})))

(defn search-results
  [{tr :tempura/tr
    :as request}
   search-term
   search-constructs]
  (let [exercises-found (search-exercises request search-term search-constructs)
        construct-labels (exercise/get-construct-labels search-constructs local-language)]
    (layout/render "search_results.html" {:exercises exercises-found
                                          :search-term search-term
                                          :search-constructs construct-labels
                                          :title (tr [:search-results/title])})))

(defroutes home-routes
  (GET "/" request (exercises-by-difficulty request))
  (context "/exercise" []
           (GET "/show/:id" [id] (show-exercise id))
           (POST "/evaluate/:id" [id :as request] (evaluate-exercise request id)))
  (context "/exercises" []
           (GET "/by-categories" request (exercises-by-categories request))
           (GET "/by-language-constructs" request (exercises-by-language-constructs request)))
  (GET "/endpoint" request (sparql-endpoint request))
  (GET "/data" request (data-page request))
  (GET "/about" request (about-page request))
  (GET "/search"
       {{search-term :q search-constructs :construct} :params
        :as request}
       (search-results request search-term search-constructs)))
