(ns sparqlab.exercise
  (:require [sparqlab.sparql :as sparql]
            [sparqlab.store :as store]
            [sparqlab.rdf :as rdf]
            [clojure.set :refer [union]]
            [clojure.string :as string]
            [clojure.tools.logging :as log])
  (:import (java.util HashMap)
           (org.apache.jena.rdf.model Model)))

(defn equal-query?
  "Test if queries `a` and `b` are equal. Starts by testing string equality, then tests equality
  of the parsed queries, then tests equality of the parsed algebras."
  [a b]
  (letfn [(equal-where? [{a :query} {b :query}]
            (.equalTo (sparql/normalize-query a) (sparql/normalize-query b) sparql/node-isomorphism-map))
          (equal-construct? [a b]
            ; If not CONSTRUCT, then skip the equality test.
            (or (not= (:query-type a) ::sparql/construct)
                (sparql/equal-construct? (:query a) (:query b))))]
    (or (= (:query-string a) (:query-string b))
      (and (= (:query-type a) (:query-type b))
           (or (= (:query a) (:query b))
               (and (equal-where? a b) (equal-construct? a b)))))))

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
  [^String lang
   prohibited
   ^Model query-model]
  (let [test-query (sparql/sparql-template "test_prohibited" {:prohibited prohibited})]
    (test-constructs lang query-model test-query)))

(defn test-required
  "Test if `required` SPARQL language constructs are used in `query`."
  [^String lang
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
        equal-query-result? (equal-query? canonical-query query)
        all-ask-queries? (every? (comp (partial = ::sparql/ask) :query-type) [canonical-query query])
        ; If the queries are the same, retrieve only the canonical results.
        results (if equal-query-result?
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
        superfluous-prohibited (test-prohibited lang prohibited query-in-spin)
        missing-required (test-required lang required query-in-spin)
        results-type (comp sparql/get-results-type :query-type)
        query-results-type (results-type query)
        canonical-results-type (results-type canonical-query)
        equal-results? (if all-ask-queries?
                         (sparql/equal-query-results? ::sparql/select (:canonical-results-to-compare results)
                                                      ::sparql/select (:query-results-to-compare results))
                         (sparql/equal-query-results? (:query-type canonical-query)
                                                      (:canonical-results results)
                                                      (:query-type query)
                                                      (:query-results results)))]
    {:canonical-query {:prefixes (sparql/extract-prefixes (:query canonical-query))
                       :query canonical-query-string
                       :query-type (:query-type canonical-query)
                       :results (:canonical-results results)
                       :results-type canonical-results-type}
     :query {:missing-required missing-required
             :prefixes (sparql/extract-prefixes (:query query))
             :query query-string
             :query-type (:query-type query)
             :results (:query-results results)
             :results-type query-results-type
             :superfluous-prohibited superfluous-prohibited}
     :equal? (and (empty? (union superfluous-prohibited missing-required))
                  (or equal-query-result? equal-results?))
     :equal-results? equal-results?}))

(defn split-prefixes
  "Split a `turtle` string to a set of prefixes and data."
  [turtle]
  (let [[prefixes data] (partition-by #(string/starts-with? % "@prefix") (string/split-lines turtle))]
    [(set prefixes) (string/trim (string/join \newline data))]))

(defn collect-prefixes
  "Collect namespace prefixes in Turtle strings `a` and `b`."
  [a b]
  (let [[a-prefixes a-data] (split-prefixes a)
        [b-prefixes b-data] (split-prefixes b)]
    [(str (string/join \newline (sort (union a-prefixes b-prefixes)))
          "\n\n"
          a-data)
     b-data]))

(defn- wrap-comment
  "Wrap `text` as block comment:
  ############
  ### text ###
  ############"
  [text]
  (let [side "###"
        line (apply str (repeat (+ (count text) 8) \#))]
    (str "\n\n" line "\n"
         side " " text " " side "\n"
         line "\n\n")))

(defn split-diff
  "Reformat query results in RDF by splitting diffs.
  Comments are localized using the `translate-fn`."
  [translate-fn query-results canonical-results]
  (let [qr (rdf/turtle-string->model query-results)
        cr (rdf/turtle-string->model canonical-results)
        ; Namespace prefixes are lost during intersection and difference operations,
        ; so we add them back.
        all-prefixes (HashMap. (merge (rdf/model-prefixes qr)
                                      (rdf/model-prefixes cr)))
        intersection (-> (.intersection qr cr)
                         (.setNsPrefixes all-prefixes)
                         rdf/model->turtle-string)
        superfluous (-> (.difference qr cr)
                        (.setNsPrefixes all-prefixes)
                        rdf/model->turtle-string)
        missing (-> (.difference cr qr)
                    (.setNsPrefixes all-prefixes)
                    rdf/model->turtle-string)
        [qr-head qr-tail] (collect-prefixes intersection superfluous)
        [cr-head cr-tail] (collect-prefixes intersection missing)
        superfluous-comment (translate-fn [:evaluation/superfluous-triples])
        missing-comment (translate-fn [:evaluation/missing-triples])]
    [(cond-> qr-head
       (seq qr-tail) (str (wrap-comment superfluous-comment) qr-tail))
     (cond-> cr-head
       (seq cr-tail) (str (wrap-comment missing-comment) cr-tail))]))

(defmulti reformat-results
  "Reformat results of SPARQL queries in exercises."
  (fn [translate-fn
       {{qrt :results-type} :query
        {crt :results-type} :canonical-query}]
    [qrt crt]))

(defmethod reformat-results ["text/turtle" "text/turtle"]
  [translate-fn
   {{query-results :results} :query
    {canonical-results :results} :canonical-query
    :keys [equal-results?]
    :as verdict}]
  (if equal-results?
    verdict
    (let [[query-results' canonical-results'] (split-diff translate-fn
                                                          query-results
                                                          canonical-results)]
      (-> verdict
          (assoc-in [:query :results] query-results')
          (assoc-in [:canonical-query :results] canonical-results')))))

; No reformatting by default.
(defmethod reformat-results :default
  [_ verdict]
  verdict)
