(ns sparqlab.util
  (:require [clojure.string :as string]
            [clojure.set :refer [difference union intersection]]))

(defn is-file?
  [file]
  (.isFile file))

(defn has-suffix?
  [suffix file]
  (string/ends-with? (.getName file) suffix))

(def query-file?
  "Predicate testing if file is named *.rq."
  (every-pred is-file? (partial has-suffix? ".rq")))

(defn ^Integer line-and-column->offset
  "Convert `line` and `column` in string `s` to offset."
  [^String s
   ^Integer line
   ^Integer column]
  (+ (->> s
          string/split-lines
          (take (dec line))
          (map (comp inc count))
          (reduce +))
     column))

(defmacro when-let*
  "<https://clojuredocs.org/clojure.core/when-let#example-5797f908e4b0bafd3e2a04bb>"
  ([bindings & body]
   (if (seq bindings)
     `(when-let [~(first bindings) ~(second bindings)]
        (when-let* ~(drop 2 bindings) ~@body))
     `(do ~@body))))

(defn select-nested-keys
  "Select nested keys via `paths` from map `m`."
  [m paths]
  (reduce (fn [out path] (update-in out path (partial get-in m path))) {} paths))

;; Kahn's topological sort. <https://gist.github.com/alandipert/1263783>
;; Copyright (c) Alan Dipert. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

; ----- Private functions -----

(defn- without
  "Returns set s with x removed."
  [s x] (difference s #{x}))

(defn- take-1
  "Returns the pair [element, s'] where s' is set s with element removed."
  [s]
  {:pre [(not (empty? s))]}
  (let [item (first s)]
    [item (without s item)]))

(defn- no-incoming
  "Returns the set of nodes in graph g for which there are no incoming
  edges, where g is a map of nodes to sets of nodes."
  [g]
  (let [nodes (set (keys g))
        have-incoming (apply union (vals g))]
    (difference nodes have-incoming)))

(defn- normalize
  "Returns g with empty outgoing edges added for nodes with incoming
  edges only.  Example: {:a #{:b}} => {:a #{:b}, :b #{}}"
  [g]
  (let [have-incoming (apply union (vals g))]
    (reduce #(if (get % %2) % (assoc % %2 #{})) g have-incoming)))

; ----- Public functions -----

(defn kahn-sort
  "Proposes a topological sort for directed graph g using Kahn's
   algorithm, where g is a map of nodes to sets of nodes. If g is
   cyclic, returns nil."
  ([g]
     (kahn-sort (normalize g) [] (no-incoming g)))
  ([g l s]
     (if (empty? s)
       (when (every? empty? (vals g)) l)
       (let [[n s'] (take-1 s)
             m (g n)
             g' (reduce #(update-in % [n] without %2) g m)]
         (recur g' (conj l n) (union s' (intersection (no-incoming g') m)))))))
