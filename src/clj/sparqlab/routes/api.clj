(ns sparqlab.routes.api
  (:require [sparqlab.config :refer [env]]
            [sparqlab.sparql :as sparql]
            [sparqlab.prefixes :as prefix]
            [sparqlab.store :as store]
            [sparqlab.cookies :as cookie]
            [clojure.tools.logging :as log]
            [compojure.core :refer [context defroutes GET]]
            [clj-http.client :as client]
            [slingshot.slingshot :refer [try+]]
            [ring.util.response :refer [response]]
            [cheshire.core :refer [generate-string]]
            [clojure.set :as set]))

(def ^:private json-header
  {"Content-Type" "application/json"})

(defn sparql-query
  "Execute a SPARQL query in an HTTP GET request using `params` and HTTP `headers`."
  [{:keys [query]
    :as params}
   headers]
  (let [{:keys [valid?]
         :as validation-results} (sparql/valid-query? query)]
    (if valid?
      (:body (client/get (:sparql-endpoint env) {:headers headers
                                                 :query-params params
                                                 :throw-entire-message? true}))
      {:status 400
       :headers json-header
       :body (generate-string validation-results)})))

(defn get-exercise-solution
  "Get canonical solution for the exercise identified with `id`."
  [request id]
  (let [exercise-status (cookie/get-exercise-status request id)
        solution (-> "get_exercise_solution"
                     (sparql/sparql-template {:exercise (prefix/exercise id)})
                     store/select-query
                     first
                     :query)]
    (cond-> (response solution)
      (not= exercise-status "solved") (cookie/mark-exercise-as-revealed id))))

(defn get-sparql-constructs
  "Get SPARQL language constructs with labels in given `lang`."
  [lang]
  (let [sparql-constructs (-> "get_sparql_constructs"
                              (sparql/sparql-template {:language lang})
                              store/select-query)
        format-construct (fn [construct] 
                           (-> construct
                               (dissoc :lang)
                               (set/rename-keys {:construct :value})))]
    {:status 200
     :headers json-header
     :body (generate-string (map format-construct sparql-constructs))}))

(defroutes api-routes
  (context "/api" []
           (GET "/query"
                {params :params
                 headers :headers}
                (sparql-query params headers))
           (GET "/exercise-solution"
                {{id :id} :params
                 :as request}
                (get-exercise-solution request id))
           (GET "/sparql-constructs"
                {lang :accept-lang}
                (get-sparql-constructs lang))))
