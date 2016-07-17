(ns user
  (:require [mount.core :as mount]
            sparqlab.core))

(defn start []
  (mount/start-without #'sparqlab.core/repl-server))

(defn stop []
  (mount/stop-except #'sparqlab.core/repl-server))

(defn restart []
  (stop)
  (start))


