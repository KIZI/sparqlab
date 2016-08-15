(ns sparqlab.test.helper
  (:require [clojure.java.io :as io]))

(defn files-in-dir
  "Files in the directory `dir`."
  [dir]
  (->> dir
       io/resource
       io/as-file
       .listFiles
       seq))
