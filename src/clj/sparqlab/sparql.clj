(ns sparqlab.sparql
  (:import [org.apache.jena.query Query QueryFactory]))

(defn equal-parsed-query?
  "Test if queries `q1` and `q2` are equal when parsed."
  [q1 q2]
  (= (QueryFactory/create q1) (QueryFactory/create q2)))

(defn equal-query?
  "Test if queries `q1` and `q2` are equal."
  [q1 q2]
  (or (= q1 q2)
      (equal-parsed-query? q1 q2)))
