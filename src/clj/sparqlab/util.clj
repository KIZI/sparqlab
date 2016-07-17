(ns sparqlab.util
  (require [clojure.string :as string]))

(def query-file?
  "Predicate testing if file is named *.rq."
  (every-pred #(.isFile %) (comp #(string/ends-with? % ".rq") #(.getName %))))
