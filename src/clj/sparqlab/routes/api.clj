(ns sparqlab.routes.api
  (:require [sparqlab.config :refer [env]]
            [sparqlab.sparql :refer [valid-query?]]
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
         :as validation-results} (valid-query? query)]
    (if valid?
      (:body (client/get (:sparql-endpoint env) {:headers headers
                                                 :query-params params
                                                 :throw-entire-message? true}))
      {:status 400
       :headers {"Content-Type" "application/json"}
       :body (generate-string (assoc (select-keys validation-results [:expected :offset])
                                     :query query))})))

(defroutes api-routes
  (context "/api" []
           (GET "/query"
                {params :params
                 headers :headers}
                (sparql-query params headers))))
