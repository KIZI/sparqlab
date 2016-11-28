(ns sparqlab.test.sparql
  (:require [sparqlab.sparql :as sparql]
            [sparqlab.test.helper :refer [files-in-dir]]
            [clojure.test :refer :all]
            [clojure.tools.logging :as log]))

(deftest valid-query?
  (testing "Invalid queries"
    (doseq [query (map slurp (files-in-dir "sparql/invalid-query"))]
      (is (not (:valid? (sparql/valid-query? query))))))
  (testing "Error offsets"
    (let [offsets {"1.rq" 244
                   "2.rq" 226
                   "3.rq" nil
                   "4.rq" 115}]
      (doseq [[filename query] (map (juxt (memfn getName) slurp)
                                    (files-in-dir "sparql/error-offsets"))
              :let [expected-offset (offsets filename)]]
        (is (= (:offset (sparql/valid-query? query)) expected-offset))))))
