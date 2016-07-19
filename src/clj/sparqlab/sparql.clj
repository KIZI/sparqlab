(ns sparqlab.sparql
  (:require [sparqlab.config :refer [env]]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [cheshire.core :refer [parse-string]])
  (:import [org.apache.jena.query Query QueryFactory]))

(derive ::describe ::construct)

(defn get-query-type
  [^Query query]
  (let [query-type (.getQueryType query)]
    (condp = query-type
      Query/QueryTypeAsk ::ask
      Query/QueryTypeSelect ::select
      Query/QueryTypeDescribe ::describe)))

(defn get-results-type
  [query-type]
  (cond (or (= query-type ::ask) (= query-type ::select)) "application/json"
        (isa? query-type ::construct) "text/turtle"))

(defn serialize-query
  "Serialize SPARQL `query` to string."
  [^Query query]
  (string/trim (.serialize query)))

(defmulti normalize-query-results (fn [query-type _] query-type))

(defmethod normalize-query-results ::ask
  [_ query-results]
  query-results)

(defmethod normalize-query-results ::construct
  [_ query-results]
  query-results)

(defmethod normalize-query-results ::select
  [_ query-results]
  (let [data (parse-string query-results keyword)]
    (cond-> (get-in data [:results :bindings])
      (not (get-in data [:results :ordered])) set ; Unordered bindings are compared as sets 
      )))

(defn sparql-query
  [endpoint query-type query]
  (let [accept (cond (or (= query-type ::select) (= query-type ::ask)) "application/sparql-results+json,*/*;q=0.9"
                     (isa? query-type ::construct) "text/turtle,*/*;q=0.9")]
    (:body (client/get endpoint {:headers {:accept accept}
                                 :query-params {"query" query}}))))

(defn equal-query?
  "Test if `query` is equal to the `canonical-query`."
  [canonical-query query]
  (let [canonical-query' (QueryFactory/create canonical-query)
        query' (QueryFactory/create query)
        equal-syntax? (or (= canonical-query query)
                          (= canonical-query' query'))
        endpoint (:sparql-endpoint env)
        canonical-query-type (get-query-type canonical-query')
        canonical-results (sparql-query endpoint canonical-query-type canonical-query)
        query-type (get-query-type query')
        query-results (if equal-syntax?
                        canonical-results
                        (sparql-query endpoint query-type query))]
    {:canonical-query {:query canonical-query
                       :results canonical-results
                       :results-type (get-results-type canonical-query-type)}
     :query {:query query
             :results query-results
             :results-type (get-results-type query-type)}
     :equal? (or equal-syntax?
                 (= (normalize-query-results canonical-query-type canonical-results)
                    (normalize-query-results query-type query-results)))}))
