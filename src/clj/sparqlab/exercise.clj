(ns sparqlab.exercise
  (:require [sparqlab.sparql :as sparql]
            [sparqlab.store :as store]
            [clojure.set :refer [union]]
            [clojure.tools.logging :as log])
  (:import (org.apache.jena.rdf.model Model)))

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
  "Get labels of `constructs` in `lang`."
  [constructs lang]
  (->> (sparql/sparql-template "get_construct_labels"
                               {:constructs constructs
                                :language lang})
       store/select-query
       (into #{})))

(defn test-constructs
  "Test SPARQL constructs on `query-model` using `test-query`."
  [^String lang
   ^Model query-model
   ^String test-query]
  (let [constructs (map :construct (sparql/select-query query-model test-query))]
    (when (seq constructs)
      (get-construct-labels constructs lang))))

(defn test-prohibited
  "Test if `prohibited` SPARQL language constructs are used in `query-model`."
  [prohibited
   ^Model query-model]
  (let [test-query (sparql/sparql-template "test_prohibited" {:prohibited prohibited})]
    (test-constructs query-model test-query)))

(defn test-required
  "Test if `required` SPARQL language constructs are used in `query`."
  [lang
   required
   ^Model query-model]
  (let [test-query (sparql/sparql-template "test_required" {:required required})]
    (test-constructs lang query-model test-query)))

(defn evaluate-exercise
  "Test if `query-string` is equal to the `canonical-query-string`."
  [^String canonical-query-string
   ^String query-string
   & {:keys [lang prohibited required]}]
  (let [sparql-query (partial sparql/sparql-query sparql/sparql-endpoint)
        canonical-query (sparql/parse-query canonical-query-string)
        query (sparql/parse-query query-string)
        equal-query-result (equal-query? canonical-query query)
        all-ask-queries? (every? (comp (partial = ::sparql/ask) :query-type) [canonical-query query])
        ; If the queries are the same, retrieve only the canonical results.
        results (if equal-query-result
                  (let [res (sparql/sparql-query-cached sparql/sparql-endpoint canonical-query)]
                    {:canonical-results res
                     :query-results res})
                  ; If the type of both queries is ASK, then rewrite them to SELECT queries
                  (if all-ask-queries?
                    (let [convert-and-query (comp sparql-query sparql/ask->select-query :query)
                          canonical-select-results (convert-and-query canonical-query)
                          query-select-results (convert-and-query query)]
                      {:canonical-results (sparql/select->ask-result canonical-select-results)
                       :canonical-results-to-compare canonical-select-results
                       :query-results (sparql/select->ask-result query-select-results)
                       :query-results-to-compare query-select-results})
                    ; Otherwise execute both queries
                    {:canonical-results (sparql-query canonical-query)
                     :query-results (sparql-query query)}))
        query-in-spin (sparql/query->spin (:query query))
        superfluous-prohibited (test-prohibited prohibited query-in-spin)
        missing-required (test-required lang required query-in-spin)]
    {:canonical-query {:query canonical-query-string
                       :results (:canonical-results results)
                       :results-type (sparql/get-results-type (:query-type canonical-query))}
     :query {:missing-required missing-required
             :query query-string
             :results (:query-results results)
             :results-type (sparql/get-results-type (:query-type query))
             :superfluous-prohibited superfluous-prohibited}
     :equal? (and (empty? (union superfluous-prohibited missing-required))
                  (or equal-query-result
                      (if all-ask-queries?
                        (sparql/equal-query-results? ::sparql/select (:canonical-results-to-compare results)
                                                     ::sparql/select (:query-results-to-compare results))
                        (sparql/equal-query-results? (:query-type canonical-query)
                                                     (:canonical-results results)
                                                     (:query-type query)
                                                     (:query-results results)))))}))
