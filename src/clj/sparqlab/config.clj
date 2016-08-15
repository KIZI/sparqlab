(ns sparqlab.config
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [args defstate]]))

(def local-language
  "Local language of the application.
  Fixed at the moment. To be obtained dynamically."
  "cs")

(defstate env :start (load-config
                       :merge
                       [(args)
                        (source/from-system-props)
                        (source/from-env)]))
