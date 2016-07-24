(ns sparqlab.prefixes)

; ----- Namespace prefixes -----

(defn- prefix
  "Builds a function for compact IRIs in the namespace `iri`."
  [iri]
  (partial str iri))

(def exercise
  (prefix "http://mynarz.net/sparqlab/resource/exercise/"))

(def local
  (prefix "http://localhost/"))

(def rdfs
  (prefix "http://www.w3.org/2000/01/rdf-schema#"))

(def xsd
  (prefix "http://www.w3.org/2001/XMLSchema#"))

(defn uuid-iri
  "Generate a UUID-based localhost IRI."
  []
  (local (str (java.util.UUID/randomUUID))))
