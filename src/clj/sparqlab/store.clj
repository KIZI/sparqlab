(ns sparqlab.store
  (:require [sparqlab.sparql :as sparql]
            [mount.core :as mount]
            [clojure.tools.logging :as log])
  (:import [org.apache.jena.rdf.model ModelFactory]))

; ----- Component -----

(defn- open-store
  []
  (let [store (ModelFactory/createDefaultModel)]
    (.read store "exercises.ttl") ; Fixture with exercises
    store))

(defn- close-store 
  [store]
  (.close store))

(mount/defstate store
  :start (open-store)
  :stop (close-store store))

(def select-query
  (partial sparql/select-query store))
