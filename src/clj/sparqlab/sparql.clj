(ns sparqlab.sparql
  (:require [sparqlab.config :refer [env]]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [cheshire.core :refer [parse-string]]
            [clojure.java.io :as io])
  (:import [org.apache.jena.query Query QueryFactory]
           [org.apache.jena.rdf.model Model]
           [org.apache.jena.query DatasetFactory]
           [org.apache.jena.riot Lang RDFDataMgr]))

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

(defn turtle-string->model
  [turtle]
  (let [input-stream (io/input-stream (.getBytes turtle))
        dataset (DatasetFactory/create)]
    (RDFDataMgr/read dataset input-stream Lang/TURTLE)
    (.getDefaultModel dataset)))

(defn serialize-query
  "Serialize SPARQL `query` to string."
  [^Query query]
  (string/trim (.serialize query)))

(defn normalize-select-results
  [query-results]
  (let [data (parse-string query-results keyword)]
    (cond-> (map (partial map val) (get-in data [:results :bindings]))
      (not (get-in data [:results :ordered])) set ; Unordered bindings are compared as sets 
      )))

(defn parse-ask-result
  [query-result]
  (:boolean (parse-string query-result keyword)))

(defmulti equal-query-results? (fn [type-1 results-1 type-2 results-2] (when (= type-1 type-2) type-1)))

(defmethod equal-query-results? ::ask
  [_ results-1 _ results-2]
  (= (parse-ask-result results-1)
     (parse-ask-result results-2)))

(defmethod equal-query-results? ::construct
  [_ results-1 _ results-2]
  (with-open [results-1' (turtle-string->model results-1)
              results-2' (turtle-string->model results-2)]
    (.isIsomorphicWith results-1' results-2')))

(defmethod equal-query-results? ::select
  [_ results-1 _ results-2]
  (= (normalize-select-results results-1)
     (normalize-select-results results-2)))

; Queries of different type don't have equal results.
(defmethod equal-query-results? :default
  [& _]
  false)

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
                 (equal-query-results? canonical-results query-results))}))
