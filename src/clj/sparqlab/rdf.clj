(ns sparqlab.rdf
  (:require [sparqlab.prefixes :as prefix]
            [sparqlab.xml-schema :as xsd]
            [clojure.string :as string])
  (:import [org.apache.jena.rdf.model Literal Resource]
           [org.apache.jena.datatypes BaseDatatype$TypedValue DatatypeFormatException]))

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

(defmethod literal->clj ::xsd/float
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
