(ns sparqlab.test.sparql
  (:require [sparqlab.sparql :as sparql]
            [sparqlab.test.helper :refer [files-in-dir]]
            [clojure.test :refer :all]
            [clojure.tools.logging :as log]))

(deftest valid-query?
  (testing "Invalid queries"
    (doseq [query (map slurp (files-in-dir "sparql/invalid-query"))]
      (is (not (:valid? (sparql/valid-query? query)))))))
