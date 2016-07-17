(ns sparqlab.routes.api
  (:require [sparqlab.config :refer [env]]
            [clojure.tools.logging :as log]
            [compojure.core :refer [context defroutes GET]]
            [clj-http.client :as client]))

(defroutes api-routes
  (context "/api" []
           (GET "/query"
                {params :params
                 headers :headers}
                (:body (client/get (:sparql-endpoint env) {:headers headers
                                                           :query-params params})))))
