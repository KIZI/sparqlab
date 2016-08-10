(ns sparqlab.rdf
  (:require [sparqlab.prefixes :as prefix]
            [sparqlab.xml-schema :as xsd]
            [clojure.string :as string]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.walk :refer [keywordize-keys]])
  (:import [java.util LinkedHashMap]
           [org.apache.jena.rdf.model Literal Model Resource]
           [org.apache.jena.datatypes BaseDatatype$TypedValue DatatypeFormatException]
           [com.github.jsonldjava.core JsonLdOptions JsonLdProcessor]
           [com.github.jsonldjava.utils JsonUtils]))

(defonce ^:private
  json-ld-options
  (doto (JsonLdOptions.) (.setUseNativeTypes true)))

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

(defn compact-json-ld
  "Compact `json-ld` with `context` using optional `options`."
  [^LinkedHashMap context
   ^LinkedHashMap json-ld
   & {:keys [options]
      :or {options json-ld-options}}]
  (JsonLdProcessor/compact json-ld context options))

(defn expand-json-ld
  "Expand `json-ld` with optional `options`."
  [^LinkedHashMap json-ld
   & {:keys [options]
      :or {options json-ld-options}}]
  (JsonLdProcessor/expand json-ld options))

(defn frame-json-ld
  "Frame `json-ld` with `frame` using optional `options`."
  [^LinkedHashMap frame
   ^LinkedHashMap json-ld
   & {:keys [options]
      :or {options json-ld-options}}]
  (JsonLdProcessor/frame json-ld frame options))

(defn model->json-ld
  [^Model model]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (.write model out "JSONLD")
    (.toString out "UTF-8")))

(defn frame-model
  "Apply JSON-LD Framing using frame from `frame-file-name` (without the file suffix)
  to the RDF `model`."
  [^Model model
   ^String frame-file-name]
  (let [frame (-> (str "json-ld-frames/" frame-file-name ".jsonld")
                  io/resource
                  io/input-stream
                  JsonUtils/fromInputStream)
        json-ld (-> model
                    model->json-ld
                    JsonUtils/fromString)]
    (-> (frame-json-ld frame json-ld)
        JsonUtils/toString
        json/parse-string
        (get "@graph")
        first
        keywordize-keys)))
