(ns sparqlab.test.util
  (:require [sparqlab.util :as util]
            [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(def map-generator
  (gen/recursive-gen (fn [inner]
                       (gen/map gen/keyword (gen/one-of [(gen/map gen/keyword inner)
                                                         (gen/vector inner)])))
                     gen/string))

(defspec linked-hash-map-round-tripping
  10
  (prop/for-all [m map-generator]
                (= (util/->clj (util/->linked-hash-map m)) m)))
