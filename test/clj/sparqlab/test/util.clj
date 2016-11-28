(ns sparqlab.test.util
  (:require [sparqlab.util :as util]
            [clojure.test :refer :all]
            [clojure.java.io :as io]))

(deftest line-and-column->offset
  (let [s (slurp (io/resource "offsets.rq"))] 
    (are [line column offset] (= (util/line-and-column->offset s line column) offset)
         11 1 246
         10 11 228
         6 12 117)))
