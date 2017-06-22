(ns sparqlab.test.exercise
  (:require [sparqlab.exercise :as exercise]
            [sparqlab.sparql :as sparql]
            [sparqlab.test.helper :as helper]
            [clojure.test :refer :all]
            [clojure.tools.logging :as log]))

(deftest equal-query?
  (testing "Equal queries"
    (doseq [[test-name [q1 q2]] (helper/load-query-pairs "sparql/equal-query")]
      (is (exercise/equal-query? (sparql/parse-query q1) (sparql/parse-query q2)) test-name)))
  (testing "Unequal queries"
    (doseq [[test-name [q1 q2]] (helper/load-query-pairs "sparql/unequal-query")]
      (is (not (exercise/equal-query? (sparql/parse-query q1) (sparql/parse-query q2))) test-name))))
