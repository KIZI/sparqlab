(ns sparqlab.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [sparqlab.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[sparqlab started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[sparqlab has shut down successfully]=-"))
   :middleware wrap-dev})
