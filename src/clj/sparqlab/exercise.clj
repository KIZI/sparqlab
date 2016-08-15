(ns sparqlab.exercise
  (:require [sparqlab.sparql :as sparql]
            [sparqlab.config :refer [env]])
  (:import [org.apache.jena.rdf.model Model]))

(defn equal-query?
  "Test if queries `a` and `b` are equal. Starts by testing string equality, then tests equality
  of the parsed queries, then tests equality of the parsed algebras."
  [a b]
  (or (= (:query-string a) (:query-string b))
      (= (:query a) (:query b))
      (.equalTo (sparql/normalize-query (:query a))
                (sparql/normalize-query (:query b))
                sparql/node-isomorphism-map)))

(defn test-prohibited
  "Test if `prohibited` SPARQL language constructs are used in `query`."
  [prohibited
   ^Model query]
  (when-let [results (->> (sparql/sparql-template "test_prohibited" {:prohibited prohibited})
                          (sparql/select-query query))]
    ))

(defn test-required
  "Test if `required` SPARQL language constructs are used in `query`."
  [required
   ^Model query]
  (when-let [results (->> (sparql/sparql-template "test_required" {:required required})
                          (sparql/select-query query))]
    ))

(defn evaluate-exercise
  "Test if `query-string` is equal to the `canonical-query-string`."
  [^String canonical-query-string
   ^String query-string
   & {:keys [prohibited required]}]
  (let [canonical-query (sparql/parse-query canonical-query-string)
        query (sparql/parse-query query-string)
        equal-query-result (equal-query? canonical-query query)
        endpoint (:sparql-endpoint env)
        canonical-results (sparql/sparql-query endpoint canonical-query)
        ; If the queries are the same, retrieve only the canonical results.
        query-results (if equal-query-result canonical-results (sparql/sparql-query endpoint query))
        query-in-spin (sparql/query->spin (:query query))
        superfluous-prohibited (test-prohibited prohibited query-in-spin)
        missing-required (test-required required query-in-spin)]
    {:canonical-query {:query (:query-string canonical-query)
                       :results canonical-results
                       :results-type (sparql/get-results-type (:query-type canonical-query))}
     :query {:query (:query-string query)
             :results query-results
             :results-type (sparql/get-results-type (:query-type query))}
     :equal? (or equal-query-result (sparql/equal-query-results? (:query-type canonical-query)
                                                                 canonical-results
                                                                 (:query-type query)
                                                                 query-results))}))
