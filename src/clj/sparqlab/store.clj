(ns sparqlab.store
  (:require [sparqlab.sparql :as sparql]
            [mount.core :as mount]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io])
  (:import [org.apache.jena.rdf.model Model ModelFactory]
           [org.apache.jena.query Query QueryFactory]))

; ----- Data pre-processing -----

(defn- parse-exercise
  [{{exercise "@id"} :exercise
    {query "@value"} :query}]
  [exercise (QueryFactory/create query)])

(defn add-prefixes
  "Add namespace prefixes used in `exercise-queries` to the `store`."
  [^Model store
   exercise-queries]
  (let [prefixes (->> exercise-queries
                      (reduce (fn [a b] (merge a (sparql/extract-prefixes (val b)))) {})
                      (map (fn [[k v]] {:prefix k :namespace v})))
        update-operation (sparql/sparql-template "add_prefixes" {:prefixes prefixes})]
    (sparql/update-operation store update-operation)))

(defn enrich-exercises-with-extracted-constructs
  "Enrich exercises in the `store` with extracted SPARQL language constructs."
  [^Model store
   exercise-queries]
  (let [extract-constructs (fn [[exercise query]]
                             (map (fn [construct] {:exercise exercise
                                                   :construct construct})
                                  (sparql/extract-language-constructs query)))
        update-operation (sparql/sparql-template "enrich_exercises_with_extracted_constructs"
                                                 {:constructs (mapcat extract-constructs exercise-queries)})]
    (sparql/update-operation store update-operation)))

(defn initialize-store
  [^Model store]
  (.read store "exercises.ttl") ; Fixture with exercises
  (.read store "vocabulary.ttl") ; SPARQLab vocabulary
  (let [exercise-queries (->> "sparql/get_exercise_queries.rq"
                              io/resource
                              slurp
                              (sparql/select-query store)
                              (map parse-exercise)
                              (into {}))]
    (-> store
        (add-prefixes exercise-queries)
        (enrich-exercises-with-extracted-constructs exercise-queries))))

; ----- Component -----

(defn- open-store
  []
  (let [store (ModelFactory/createDefaultModel)]
    (initialize-store store)))

(defn- close-store 
  [store]
  (.close store))

(mount/defstate store
  :start (open-store)
  :stop (close-store store))

(defn construct-query
  "Execute CONSTRUCT `query` on the store."
  [query]
  (sparql/construct-query store query))

(defn select-query
  "Execute SELECT `query` on the store."
  [query]
  (sparql/select-query store query))
