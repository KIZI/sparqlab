(ns sparqlab.handler
  (:require [sparqlab.config :refer [env]]
            [sparqlab.env :refer [defaults]]
            [sparqlab.layout :as layout]
            [sparqlab.middleware :as middleware]
            [sparqlab.routes.home :refer [home-routes]]
            [sparqlab.routes.api :refer [api-routes]]
            [clojure.tools.logging :as log]
            [compojure.core :refer [rfn routes wrap-routes]]
            [compojure.route :as route]
            [mount.core :as mount]))

(mount/defstate init-app
                :start ((or (:init defaults) identity))
                :stop  ((or (:stop defaults) identity)))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (doseq [component (:started (mount/start))]
    (log/info component "started")))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents)
  (log/info "sparqlab has shut down!"))

(def app-routes
  (routes
    (-> #'home-routes
        (wrap-routes middleware/wrap-formats))
    #'api-routes
    (rfn request (layout/not-found request))))

(def app (middleware/wrap-base #'app-routes))
