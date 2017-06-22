(ns sparqlab.test.helper
  (:require [sparqlab.util :as util]
            [clojure.java.io :as io]))

(defn files-in-dir
  "Files in the directory `dir`."
  [dir]
  (->> dir
       io/resource
       io/as-file
       .listFiles
       seq))

(defn load-query-pairs
  "Load query pairs from `dir`."
  [dir]
  (->> (files-in-dir dir)
       (map (juxt #(.getName %)
                  (comp (partial map slurp)
                        (partial filter util/query-file?)
                        seq
                        #(.listFiles %))))
       (into {})))
