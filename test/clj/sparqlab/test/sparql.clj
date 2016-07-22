(ns sparqlab.test.sparql
  (:require [clojure.test :refer :all]
            [sparqlab.sparql :as sparql]
            [sparqlab.util :refer [query-file?]]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]))

(deftest equal-query?
  (letfn [(load-query-pairs [dir]
            (map (comp (partial map slurp)
                       (partial filter query-file?)
                       seq
                       #(.listFiles %))
                 (->> dir
                      io/resource
                      io/as-file
                      .listFiles
                      seq)))]
    (testing "Equal queries"
      (doseq [[q1 q2] (load-query-pairs "sparql/equal-query")]
        (is (sparql/equal-query? (sparql/parse-query q1) (sparql/parse-query q2)))))
    (testing "Unequal queries"
      (doseq [[q1 q2] (load-query-pairs "sparql/unequal-query")]
        (is (not (sparql/equal-query? (sparql/parse-query q1) (sparql/parse-query q2))))))))

(deftest equal-results?)
