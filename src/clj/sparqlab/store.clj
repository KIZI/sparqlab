(ns sparqlab.store
  (:require [sparqlab.sparql :as sparql]
            [mount.core :as mount]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io])
  (:import [org.apache.jena.rdf.model ModelFactory]
           [org.apache.jena.query QueryFactory]))

; ----- Data pre-processing -----

(defn enrich-exercises-with-extracted-constructs
  [store]
  (let [exercise-queries (->> "sparql/get_exercise_queries.rq"
                              io/resource
                              slurp
                              (sparql/select-query store))
        extract-constructs (fn [{{exercise "@id"} :exercise
                                 {query "@value"} :query}]
                             (map #(assoc {:exercise exercise} :construct %)
                                  (sparql/extract-language-constructs (QueryFactory/create query))))
        update-operation (sparql/sparql-template "enrich_exercises_with_extracted_constructs"
                                                 {:constructs (mapcat extract-constructs exercise-queries)})]
    (sparql/update-operation store update-operation)))

; ----- Component -----

(defn- open-store
  []
  (let [store (ModelFactory/createDefaultModel)]
    (.read store "exercises.ttl") ; Fixture with exercises
    (enrich-exercises-with-extracted-constructs store)))

(defn- close-store 
  [store]
  (.close store))

(mount/defstate store
  :start (open-store)
  :stop (close-store store))

(def select-query
  (partial sparql/select-query store))
