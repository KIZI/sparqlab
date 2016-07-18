(ns sparqlab.test.sparql
  (:require [clojure.test :refer :all]
            [sparqlab.sparql :as sparql]
            [sparqlab.util :refer [query-file?]]
            [clojure.java.io :as io]))

(deftest equal-query?
  (let [query-pairs (map (comp (partial map slurp) seq #(.listFiles %))
                         (->> "sparql/equal-query"
                              io/resource
                              io/as-file
                              .listFiles
                              seq
                              (filter query-file?)))]
    (doseq [[q1 q2] query-pairs]
      (is (:equal? (sparql/equal-query? q1 q2))))))

(deftest equal-results?)
