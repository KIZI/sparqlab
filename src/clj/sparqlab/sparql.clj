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
  "Convert RDF in Turtle to an in-memory RDF model."
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
  "Normalize results of a SPARQL SELECT query."
  [query-results]
  (let [data (parse-string query-results keyword)]
    (cond-> (map (partial map val) (get-in data [:results :bindings]))
      (not (get-in data [:results :ordered])) set ; Unordered bindings are compared as sets 
      )))

(defn parse-ask-result
  "Parse SPARQL ASK query."
  [query-result]
  (:boolean (parse-string query-result keyword)))

(defmulti equal-query-results?
  "Test if query results are equal. Dispatches on the type of the first query results.
  If the query types of the compared query results don't match, results are treated as unequal."
  ; Query types are compared with `isa?` to allow comparing DESCRIBE and CONSTRUCT.
  (fn [type-1 results-1 type-2 results-2] (when (or (isa? type-1 type-2) (isa? type-2 type-1)) type-1)))

(defmethod equal-query-results? ::ask
  [_ results-1 _ results-2]
  (= (parse-ask-result results-1) (parse-ask-result results-2)))

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
  "Send a SPARQL query to `endpoint`."
  [^String endpoint
   {:keys [query-string query-type]}]
  (let [accept (cond (#{::ask ::select} query-type) "application/sparql-results+json,*/*;q=0.9"
                     (isa? query-type ::construct) "text/turtle,*/*;q=0.9")]
    (:body (client/get endpoint {:headers {:accept accept}
                                 :query-params {"query" query-string}}))))

(defn parse-query
  "Parse a query string."
  [^String query]
  (let [parsed-query (QueryFactory/create query)]
    {:query parsed-query
     :query-string query
     :query-type (get-query-type parsed-query)}))

(defn equal-query?
  "Test if queries `a` and `b` are equal. Starts by testing string equality, then tests equality
  of the parsed queries."
  [a b]
  (or (= (:query-string a) (:query-string b))
      (= (:query a) (:query b))))

(defn evaluate-exercise
  "Test if `query-string` is equal to the `canonical-query-string`."
  [^String canonical-query-string
   ^String query-string]
  (let [canonical-query (parse-query canonical-query-string)
        query (parse-query query-string)
        equal-query-result (equal-query? canonical-query query)
        endpoint (:sparql-endpoint env)
        canonical-results (sparql-query endpoint canonical-query)
        ; If the queries are the same, retrieve only the canonical results.
        query-results (if equal-query-result canonical-results (sparql-query endpoint query))]
    {:canonical-query {:query canonical-query-string
                       :results canonical-results
                       :results-type (get-results-type (:query-type canonical-query))}
     :query {:query query-string
             :results query-results
             :results-type (get-results-type (:query-type query))}
     :equal? (or equal-query-result (equal-query-results? (:query-type canonical-query)
                                                          canonical-results
                                                          (:query-type query)
                                                          query-results))}))
