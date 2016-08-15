(ns sparqlab.test.exercise
  (:require [sparqlab.exercise :as exercise]
            [sparqlab.sparql :as sparql]
            [sparqlab.util :as util]
            [sparqlab.test.helper :refer [files-in-dir]]
            [clojure.test :refer :all]
            [clojure.tools.logging :as log]))

(defn load-query-pairs
  [dir]
  (->> (files-in-dir dir)
       (map (juxt #(.getName %)
                  (comp (partial map slurp)
                        (partial filter util/query-file?)
                        seq
                        #(.listFiles %))))
       (into {})))

(deftest equal-query?
  (testing "Equal queries"
    (doseq [[test-name [q1 q2]] (load-query-pairs "sparql/equal-query")]
      (is (exercise/equal-query? (sparql/parse-query q1) (sparql/parse-query q2)) test-name)))
  (testing "Unequal queries"
    (doseq [[test-name [q1 q2]] (load-query-pairs "sparql/unequal-query")]
      (is (not (exercise/equal-query? (sparql/parse-query q1) (sparql/parse-query q2))) test-name))))
