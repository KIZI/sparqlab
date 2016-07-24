(ns sparqlab.test.sparql
  (:require [clojure.test :refer :all]
            [sparqlab.sparql :as sparql]
            [sparqlab.util :refer [query-file?]]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.edn :as edn]))

(defn- files-in-dir
  "Files in the directory `dir`."
  [dir]
  (->> dir
       io/resource
       io/as-file
       .listFiles
       seq))

(deftest equal-query?
  (letfn [(load-query-pairs [dir]
            (map (comp (partial map slurp)
                       (partial filter query-file?)
                       seq
                       #(.listFiles %))
                 (files-in-dir dir)))]
    (testing "Equal queries"
      (doseq [[q1 q2] (load-query-pairs "sparql/equal-query")]
        (is (sparql/equal-query? (sparql/parse-query q1) (sparql/parse-query q2)))))
    (testing "Unequal queries"
      (doseq [[q1 q2] (load-query-pairs "sparql/unequal-query")]
        (is (not (sparql/equal-query? (sparql/parse-query q1) (sparql/parse-query q2))))))))

(deftest equal-results?)

(deftest extract-language-constructs
  (let [dir "sparql/extract-language-constructs"
        slurp-file (comp slurp io/resource (partial str dir "/"))
        get-query-and-constructs (fn [file-name]
                                   [(slurp-file (str file-name ".rq"))
                                    (edn/read-string (slurp-file (str file-name ".edn")))])
        queries-and-constructs (->> (files-in-dir dir)
                                    (map #(-> % (.getName) (string/split #"\.") first))
                                    distinct
                                    (map get-query-and-constructs))]
    (doseq [[query constructs] queries-and-constructs]
      (is (= (sparql/extract-language-constructs query) constructs)))))
