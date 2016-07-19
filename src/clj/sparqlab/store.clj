(ns sparqlab.store
  (:require [sparqlab.prefixes :as prefix]
            [sparqlab.xml-schema :as xsd]
            [mount.core :as mount]
            [clojure.tools.logging :as log]
            [clojure.string :as string])
  (:import [org.apache.jena.rdf.model Literal ModelFactory Resource]
           [org.apache.jena.query QueryExecutionFactory]
           [org.apache.jena.datatypes BaseDatatype$TypedValue DatatypeFormatException]
           [org.apache.jena.datatypes.xsd XSDDatatype]))

(defn xml-schema-data-type?
  "Predicate testing if `data-type` is from XML Schema."
  [^String data-type]
  (string/starts-with? data-type (prefix/xsd)))

(defn data-type->xml-schema
  "Coerce a XML Schema `data-type`."
  [^String data-type]
  (keyword "sparqlab.xml-schema" (string/replace data-type (prefix/xsd) "")))

(defmulti literal->clj
  "Convert RDF literal to a Clojure scalar data type."
  (fn [{datatype "@type"}]
    (when (xml-schema-data-type? datatype)
      (data-type->xml-schema datatype))))

(defmethod literal->clj ::xsd/duration
  [literal]
  (update literal "@value" str))

(defmethod literal->clj ::xsd/integer
  ; Prevent casting integers to doubles.
  [literal]
  literal)

(defmethod literal->clj ::xsd/decimal
  [literal]
  (update literal "@value" double))

(defmethod literal->clj ::xsd/date
  [literal]
  (update literal "@value" str))

(defmethod literal->clj ::xsd/dateTime
  [literal]
  (update literal "@value" str))

(defmethod literal->clj :default
  [{value "@value"
    :as literal}]
  (if (instance? BaseDatatype$TypedValue value)
    (update literal "@value" #(.lexicalValue %))
    literal))

(defprotocol RDFResource
  "Returns a Clojuresque representation of an RDF resource"
  (resource->clj [resource]))

(extend-protocol RDFResource
  Literal
  (resource->clj [resource]
    (let [datatype (.getDatatypeURI resource)]
      (literal->clj (try (cond-> {"@value" (.getValue resource)}
                           datatype (assoc "@type" datatype))
                         (catch DatatypeFormatException _
                           ; Treat invalid literals as strings
                           {"@value" (.getLexicalForm resource)
                            "@type" (prefix/xsd "string")})))))

  Resource
  (resource->clj [resource]
    {"@id" (if (.isAnon resource)
             (str "_:" (.getId resource))
             (.getURI resource))}))

; ----- Private functions -----

(defn- process-select-binding
  [sparql-binding variable]
  [(keyword variable) (resource->clj (.get sparql-binding variable))])

(defn- process-select-solution
  "Process SPARQL SELECT `solution` for `result-vars`."
  [result-vars solution]
  (into {} (mapv (partial process-select-binding solution) result-vars)))

; ----- Component -----

(defn- open-store
  []
  (let [store (ModelFactory/createDefaultModel)]
    (.read store "exercises.ttl") ; Fixture with exercises
    store))

(defn- close-store 
  [store]
  (.close store))

(mount/defstate store
  :start (open-store)
  :stop (close-store store))

; ----- Public functions -----

(defn select-query 
  "Execute SPARQL SELECT `query` on the local store"
  [^String query]
  (with-open [qexec (QueryExecutionFactory/create query store)]
    (let [results (.execSelect qexec)
          result-vars (.getResultVars results)]
      (mapv (partial process-select-solution result-vars)
            (iterator-seq results)))))
