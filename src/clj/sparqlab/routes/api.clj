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
            [cheshire.core :refer [generate-string]]))

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
       :headers {"Content-Type" "application/json"}
       :body (generate-string (assoc (select-keys validation-results [:expected :offset])
                                     :query query))})))

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

(defroutes api-routes
  (context "/api" []
           (GET "/query"
                {params :params
                 headers :headers}
                (sparql-query params headers))
           (GET "/exercise-solution"
                {{id :id} :params
                 :as request}
                (get-exercise-solution request id))))
