(ns sparqlab.util
  (require [clojure.string :as string]))

(defn is-file?
  [file]
  (.isFile file))

(defn has-suffix?
  [suffix file]
  (string/ends-with? (.getName file) suffix))

(def query-file?
  "Predicate testing if file is named *.rq."
  (every-pred is-file? (partial has-suffix? ".rq")))
