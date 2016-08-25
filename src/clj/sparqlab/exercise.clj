(ns sparqlab.exercise
  (:require [sparqlab.sparql :as sparql]
            [sparqlab.store :as store]
            [sparqlab.config :refer [local-language]]
            [clojure.set :refer [union]]
            [clojure.tools.logging :as log])
  (:import [org.apache.jena.rdf.model Model]))

(defn equal-query?
  "Test if queries `a` and `b` are equal. Starts by testing string equality, then tests equality
  of the parsed queries, then tests equality of the parsed algebras."
  [a b]
  (or (= (:query-string a) (:query-string b))
      (and (= (:query-type a) (:query-type b)) (= (:query a) (:query b)))
      (.equalTo (sparql/normalize-query (:query a))
                (sparql/normalize-query (:query b))
                sparql/node-isomorphism-map)))

(defn get-construct-labels
  "Get labels of `constructs` in `local-language`."
  [constructs local-language]
  (->> (sparql/sparql-template "get_construct_labels"
                               {:constructs constructs
                                :language local-language})
       store/select-query
       (into #{})))

(defn test-constructs
  "Test SPARQL constructs on `query-model` using `test-query`."
  [^Model query-model
   ^String test-query]
  (let [constructs (map :construct (sparql/select-query query-model test-query))]
    (when (seq constructs)
      (get-construct-labels constructs local-language))))

(defn test-prohibited
  "Test if `prohibited` SPARQL language constructs are used in `query-model`."
  [prohibited
   ^Model query-model]
  (let [test-query (sparql/sparql-template "test_prohibited" {:prohibited prohibited})]
    (test-constructs query-model test-query)))

(defn test-required
  "Test if `required` SPARQL language constructs are used in `query`."
  [required
   ^Model query-model]
  (let [test-query (sparql/sparql-template "test_required" {:required required})]
    (test-constructs query-model test-query)))

(defn evaluate-exercise
  "Test if `query-string` is equal to the `canonical-query-string`."
  [^String canonical-query-string
   ^String query-string
   & {:keys [prohibited required]}]
  (let [canonical-query (sparql/parse-query canonical-query-string)
        query (sparql/parse-query query-string)
        equal-query-result (equal-query? canonical-query query)
        canonical-results (sparql/sparql-query-cached sparql/sparql-endpoint canonical-query)
        ; If the queries are the same, retrieve only the canonical results.
        query-results (if equal-query-result
                        canonical-results
                        (sparql/sparql-query sparql/sparql-endpoint query))
        query-in-spin (sparql/query->spin (:query query))
        superfluous-prohibited (test-prohibited prohibited query-in-spin)
        missing-required (test-required required query-in-spin)]
    {:canonical-query {:query (:query-string canonical-query)
                       :results canonical-results
                       :results-type (sparql/get-results-type (:query-type canonical-query))}
     :query {:missing-required missing-required
             :query (:query-string query)
             :results query-results
             :results-type (sparql/get-results-type (:query-type query))
             :superfluous-prohibits superfluous-prohibited}
     :equal? (and (empty? (union superfluous-prohibited missing-required))
                  (or equal-query-result
                      (sparql/equal-query-results? (:query-type canonical-query)
                                                   canonical-results
                                                   (:query-type query)
                                                   query-results)))}))
