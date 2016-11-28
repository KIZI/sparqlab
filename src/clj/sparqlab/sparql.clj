(ns sparqlab.sparql
  (:require [sparqlab.prefixes :refer [uuid-iri]]
            [sparqlab.rdf :refer [resource->clj]]
            [sparqlab.util :as util]
            [sparqlab.config :refer [env]]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [cheshire.core :as json] 
            [clojure.java.io :as io]
            [stencil.core :refer [render-file]]
            [stencil.loader :refer [set-cache]]
            [mount.core :as mount])
  (:import (java.io StringReader)
           (java.net SocketException)
           (org.apache.jena.query ARQ DatasetFactory Query QueryExecutionFactory
                                  QueryFactory QueryParseException Syntax)
           (org.apache.jena.update UpdateAction UpdateFactory)
           (org.apache.jena.rdf.model Model ModelFactory)
           (org.apache.jena.riot Lang RDFDataMgr)
           (org.apache.jena.sparql.algebra Algebra Op)
           (org.apache.jena.sparql.algebra.optimize Optimize TransformMergeBGPs)
           (org.apache.jena.sparql.util Context NodeIsomorphismMap)
           (org.apache.jena.sparql.core Var)
           (org.apache.jena.graph Node)
           (org.topbraid.spin.arq ARQ2SPIN)
           (org.apache.jena.sparql.lang SyntaxVarScope)
           (org.apache.jena.sparql.lang.sparql_11 ParseException SPARQLParser11 Token)
           (org.aksw.jena_sparql_api.algebra.transform TransformDistributeJoinOverUnion)))

(def arq-context
  "Context allowing more aggressive SPARQL query optimizations."
  (doto (.copy (ARQ/getContext))
    (.set ARQ/optInlineAssignmentsAggressive true)))

(def node-isomorphism-map
  "Make variables and blank nodes isomorphic."
  (let [m (atom {})]
    (proxy [NodeIsomorphismMap] []
      (makeIsomorphic [^Node n1 ^Node n2]
        (if (and (or (Var/isBlankNodeVar n1) (.isBlank n1))
                 (or (Var/isBlankNodeVar n2) (.isBlank n2)))
          (let [other (@m n1)]
            (if-not other
              (do (swap! m #(assoc % n1 n2)) true)
              (.equals other n2)))
          (.equals n1 n2))))))

; Disable Stencil's caching for testing
(set-cache (clojure.core.cache/ttl-cache-factory {} :ttl 0))

(derive ::describe ::construct)

; ----- Private functions -----

(defn ->plain-literal
  "Convert `variable` to plain literal."
  [{id "@id"
    value "@value"}]
  (or value id))

(defn- process-select-binding
  [sparql-binding variable]
  [(keyword variable) (->plain-literal (resource->clj (.get sparql-binding variable)))])

(defn- process-select-solution
  "Process SPARQL SELECT `solution` for `result-vars`."
  [result-vars solution]
  (into {} (mapv (partial process-select-binding solution) result-vars)))

(defn query->spin
  "Convert SPARQL `query` to SPIN RDF model."
  [^Query query]
  (let [model (ModelFactory/createDefaultModel)]
    (.. (ARQ2SPIN. model) (createQuery query (uuid-iri)) (getModel))))

(defn extract-prefixes
  "Extract namespace prefixes used in exercise `query`."
  [^Query query]
  (into {} (.. query getPrologue getPrefixMapping getNsPrefixMap)))

(defn get-query-type
  [^Query query]
  (let [query-type (.getQueryType query)]
    (condp = query-type
      Query/QueryTypeAsk ::ask
      Query/QueryTypeConstruct ::construct
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

(defn- get-error-offset
  "Compute offset of a syntax `exception` in `query` based on line and column numbers."
  [^String query
   ^ParseException exception]
  (let [current-token (.currentToken exception)]
    (+ (util/line-and-column->offset query
                                     (.beginLine current-token)
                                     (.beginColumn current-token))
       (count (.image current-token))))) ; Number of characters in the preceding token

(defn- get-expected-tokens
  "Get string representations of the tokens expected by `exception`."
  [^ParseException exception]
  (let [token-images (.tokenImage exception)
        idxs (flatten (mapv vec (.expectedTokenSequences exception)))]
    (mapv (partial aget token-images) idxs)))

(defn valid-query?
  "Validates syntax of `query`." 
  [^String query]
  (let [parsed-query (doto (Query.) (.setStrict true))
        parser (doto (SPARQLParser11. (StringReader. query))
                 (.setQuery parsed-query))]
    (try (do (.QueryUnit parser)
             (SyntaxVarScope/check parsed-query)
             {:valid? true})
         (catch ParseException ex
           {:expected (get-expected-tokens ex)
            :offset (get-error-offset query ex)
            :query query
            :valid? false})
         (catch QueryParseException ex
           {:message (.getMessage ex)
            :valid? false}))))

(defn transform-distribute-join-over-union
  [^Op op]
  (Optimize/apply (TransformMergeBGPs.) (TransformDistributeJoinOverUnion/transform op)))

(defn normalize-query
  [^Query query]
  (-> query
      Algebra/compile
      transform-distribute-join-over-union
      (Algebra/optimize arq-context)))

(defn normalize-select-results
  "Normalize results of a SPARQL SELECT query."
  [query-results]
  (let [data (json/parse-string query-results keyword)]
    (cond-> (map (partial map val) (get-in data [:results :bindings]))
      (not (get-in data [:results :ordered])) set ; Unordered bindings are compared as sets 
      )))

(defn parse-ask-result
  "Parse SPARQL ASK query."
  [query-result]
  (:boolean (json/parse-string query-result keyword)))

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

(def sparql-query-cached
  "Send a SPARQL query to `endpoint` and cache its results."
  (memoize sparql-query))

(defn get-construct-template
  "Extract CONSTRUCT template from `query`.
  Returns nil for non-CONSTRUCT queries."
  [^String query]
  (when-let [[_ construct-clause] (re-find (re-matcher #"(?is)^(.*CONSTRUCT\s*(\{[^}]+\})?).*$" query))]
    (str construct-clause \newline "WHERE {\n}")))

(defn parse-query
  "Parse a query string."
  [^String query]
  (let [parsed-query (QueryFactory/create query)]
    {:query parsed-query
     :query-string (serialize-query parsed-query)
     :query-type (get-query-type parsed-query)}))

(defn select-query 
  "Execute SPARQL SELECT `query` on the `model`."
  [^Model model
   ^String query]
  (with-open [qexec (QueryExecutionFactory/create query model)]
    (let [results (.execSelect qexec)
          result-vars (.getResultVars results)]
      (mapv (partial process-select-solution result-vars)
            (iterator-seq results)))))

(defn ^Model construct-query
  "Execute SPARQL CONSTRUCT `query` on the `model`.
  Returns expanded JSON-LD."
  [^Model model
   ^String query]
  (with-open [qexec (QueryExecutionFactory/create query model)]
    (.execConstruct qexec)))

(defn ^Model update-operation
  "Execute SPARQL Update `operation` on `model`."
  [^Model model
   ^String operation]
  (UpdateAction/execute (UpdateFactory/create operation) model)
  model)

(defn sparql-template
  "Render a SPARQL template from `file-name` using optional `data`."
  ([file-name]
   (sparql-template file-name {}))
  ([file-name data]
   (render-file (str "sparql/" file-name ".mustache") data)))

(defn ask->select-query
  "Convert ASK `query` to SELECT query."
  [^Query query]
  (let [query-pattern (.getQueryPattern query)
        select (doto (Query.)
                 (.setQuerySelectType)
                 (.setQueryResultStar true)
                 (.setQueryPattern query-pattern)
                 (.setLimit 100))]
    {:query select
     :query-string (serialize-query select)
     :query-type ::select}))

(defn ask-result
  "Make a result of SPARQL ASK query"
  [^Boolean result]
  (let [response {:head {}
                  :boolean result}]
    (json/generate-string response {:pretty true})))

(defn select->ask-result
  "Convert SPARQL SELECT results as if they were results for SPARQL ASK query."
  [^String select-results]
  (-> select-results
      (json/parse-string keyword)
      (get-in [:results :bindings])
      empty?
      not
      ask-result))

(defn ping-endpoint
  "Test if SPARQL endpoint at `endpoint-url` can be reached."
  [endpoint-url]
  (try (assert (parse-ask-result (sparql-query endpoint-url {:query-string (sparql-template "ping_endpoint")
                                                             :query-type ::ask}))
               (str "SPARQL endpoint <" endpoint-url "> contains no data."))
       (catch SocketException _
         (throw (Exception. (str "SPARQL endpoint <" endpoint-url "> is unreachable."))))))

(defn init-endpoint
  "Initialize a remote SPARQL endpoint."
  []
  (let [endpoint-url (:sparql-endpoint env)]
    (ping-endpoint endpoint-url)
    endpoint-url))

(mount/defstate sparql-endpoint
  :start (init-endpoint))
