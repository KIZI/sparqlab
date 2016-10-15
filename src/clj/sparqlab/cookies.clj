(ns sparqlab.cookies
  (:require [clojure.string :as string]))

(def a-year
  "One year in seconds"
  (* 60 60 24 365))

(def cookie-ns
  "sparqlab-exercise-")

(defn set-cookie
  [response k value & {:as data}]
  (assoc-in response
            [:cookies k]
            (merge {:max-age a-year
                    :path "/"}
                   data
                   {:value value})))

(defn mark-exercise-status
  "Mark exercise identified with `id` with a `status`."
  [status response id]
  (set-cookie response (str cookie-ns id) status))

(def mark-exercise-as-solved
  "Mark exercise identified with `id` as solved using a cookie."
  (partial mark-exercise-status "solved"))

(def mark-exercise-as-revealed
  "Mark exercise identified with `id` as revealed using a cookie."
  (partial mark-exercise-status "revealed"))

(defn get-exercise-status
  "Get status of the exercise identified with `id`."
  [request id]
  (get-in request [:cookies (str cookie-ns id) :value]))

(defn get-exercise-statuses
  "Get a map of exercise IDs to their statuses (from #{solved, revealed})."
  [request]
  (->> (:cookies request)
       (filter (comp #(string/starts-with? % cookie-ns) key))
       (map (juxt (comp #(subs % (count cookie-ns)) key)
                  (comp :value val)))
       (into {})))
