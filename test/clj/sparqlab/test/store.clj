(ns sparqlab.test.store
  (:require [sparqlab.store :as store]
            [clojure.test :refer :all]
            [clojure.java.io :as io]
            [mount.core :as mount]
            [clojure.tools.logging :as log])
  (:import [org.apache.jena.rdf.model ModelFactory]))

(use-fixtures :once
              (fn [f]
                (mount/start-with {#'store/store (ModelFactory/createDefaultModel)})
                (f)
                (mount/stop)))

(deftest resource->clj
  (let [query (slurp (io/resource "sparql/resource_to_clj.rq"))
        results (store/select-query query)]
    (is results "Conversion of RDF resources to Clojure data types does not throw an error.")
    #_(log/info (distinct (map (comp type #(get-in % [:resource "@value"])) results)))
    ))
