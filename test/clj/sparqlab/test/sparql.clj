(ns sparqlab.test.sparql
  (:require [clojure.test :refer :all]
            [sparqlab.sparql :as sparql]
            [sparqlab.util :as util]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.edn :as edn])
  (:import [org.apache.jena.query QueryFactory]))

(defn- files-in-dir
  "Files in the directory `dir`."
  [dir]
  (->> dir
       io/resource
       io/as-file
       .listFiles
       seq))

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
      (is (sparql/equal-query? (sparql/parse-query q1) (sparql/parse-query q2)) test-name)))
  (testing "Unequal queries"
    (doseq [[test-name [q1 q2]] (load-query-pairs "sparql/unequal-query")]
      (is (not (sparql/equal-query? (sparql/parse-query q1) (sparql/parse-query q2))) test-name))))

(deftest equal-results?)

(deftest extract-language-constructs
  (let [dir "sparql/extract-language-constructs"
        slurp-file (comp slurp io/resource (partial str dir "/"))
        get-query-and-constructs (fn [file-name]
                                   [(QueryFactory/create (slurp-file (str file-name ".rq")))
                                    (edn/read-string (slurp-file (str file-name ".edn")))])
        queries-and-constructs (->> (files-in-dir dir)
                                    (filter (every-pred util/is-file?
                                                        (some-fn (partial util/has-suffix? ".rq")
                                                                 (partial util/has-suffix? ".edn"))))
                                    (map #(-> % (.getName) (string/split #"\.") first))
                                    distinct
                                    (map get-query-and-constructs))]
    (doseq [[query constructs] queries-and-constructs]
      (let [extracted (sparql/extract-language-constructs query)]
        (is (= extracted constructs))))))
