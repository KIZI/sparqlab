(ns sparqlab.prefixes)

; ----- Namespace prefixes -----

(defn- prefix
  "Builds a function for compact IRIs in the namespace `iri`."
  [iri]
  (partial str iri))

(def dcterms
  (prefix "http://purl.org/dc/terms/"))

(def exercise
  (prefix "http://mynarz.net/sparqlab/resource/exercise/"))

(def local
  (prefix "http://localhost/"))

(def rdfs
  (prefix "http://www.w3.org/2000/01/rdf-schema#"))

(def skos
  (prefix "http://www.w3.org/2004/02/skos/core#"))

(def sp
  (prefix "http://spinrdf.org/sp#"))

(def sparqlab
  (prefix "http://mynarz.net/sparqlab/vocabulary#"))

(def xsd
  (prefix "http://www.w3.org/2001/XMLSchema#"))

(defn uuid-iri
  "Generate a UUID-based localhost IRI."
  []
  (local (str (java.util.UUID/randomUUID))))
