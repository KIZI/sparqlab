(ns sparqlab.util
  (:require [clojure.string :as string])
  (:import [clojure.lang IPersistentMap IPersistentVector]
           [java.util ArrayList LinkedHashMap]))

(defn is-file?
  [file]
  (.isFile file))

(defn has-suffix?
  [suffix file]
  (string/ends-with? (.getName file) suffix))

(def query-file?
  "Predicate testing if file is named *.rq."
  (every-pred is-file? (partial has-suffix? ".rq")))

(defprotocol LinkedHashMappable
  "Convert Clojure data structure to LinkedHashMap."
  (->linked-hash-map [data]))

(extend-protocol LinkedHashMappable
  IPersistentMap
  (->linked-hash-map [m]
    (let [lhm (LinkedHashMap.)]
      (doseq [[k v] m]
        (.put lhm
              (if (keyword? k) (name k) k)
              (->linked-hash-map v)))
      lhm))

  IPersistentVector
  (->linked-hash-map [v]
    (let [alist (ArrayList.)]
      (doseq [i v] (.add alist (->linked-hash-map i)))
      alist))
  
  Object
  (->linked-hash-map [o] o))

(defprotocol Clojurizable
  "Convert LinkedHashMap to Clojure data structure."
  (->clj [lhm]))

(extend-protocol Clojurizable
  LinkedHashMap
  (->clj [lhm]
    (reduce (fn [m [k v]]
              (assoc m (keyword k) (->clj v)))
            {}
            (iterator-seq (.. lhm entrySet iterator))))

  ArrayList
  (->clj [alist]
    (mapv ->clj alist))

  Object
  (->clj [o] o))
