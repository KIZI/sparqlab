(ns sparqlab.test.store
  (:require [sparqlab.store :as store]
            [sparqlab.test.helper :refer [files-in-dir]]
            [clojure.test :refer :all]
            [sparqlab.util :as util]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.edn :as edn])
  (:import [org.apache.jena.query QueryFactory]))

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
      (let [extracted (store/extract-language-constructs query)]
        (is (= extracted constructs))))))
