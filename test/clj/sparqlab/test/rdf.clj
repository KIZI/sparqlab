(ns sparqlab.test.rdf
  (:require [sparqlab.rdf :as rdf]
            [sparqlab.sparql :as sparql]
            [clojure.test :refer :all]
            [clojure.java.io :as io])
  (:import [org.apache.jena.rdf.model ModelFactory]
           [org.apache.jena.datatypes.xsd XSDDatatype]))

(deftest resource->clj
  (with-open [model (ModelFactory/createDefaultModel)]
    (let [query (slurp (io/resource "sparql/resource_to_clj.rq"))
          results (sparql/select-query model query)]
      (is results "Conversion of RDF resources to Clojure data types does not throw an error."))
    (testing "Conversion of literals"
      (testing "Conversion of plain literals"
        (are [literal clj] (= (get (rdf/resource->clj literal) "@value") clj)
             (.createLiteral model "word") "word"
             (.createLiteral model "word" "en") "word"))
      (testing "Conversion of typed literals"
        (are [literal datatype clj] (= (get (rdf/resource->clj (.createTypedLiteral model
                                                                                    literal
                                                                                    datatype))
                                            "@value") clj)
             "word" XSDDatatype/XSDstring "word"
             "true" XSDDatatype/XSDboolean true
             "123" XSDDatatype/XSDinteger 123
             "-1.1" XSDDatatype/XSDdecimal -1.1
             "INF" XSDDatatype/XSDfloat Double/POSITIVE_INFINITY
             "12.78e-2" XSDDatatype/XSDfloat (double (float 0.1278))
             "http://www.w3.org/2001/XMLSchema#" XSDDatatype/XSDanyURI "http://www.w3.org/2001/XMLSchema#"
             "P5Y" XSDDatatype/XSDduration "P5Y"
             "2002-10-10T12:00:00Z" XSDDatatype/XSDdateTime "2002-10-10T12:00:00Z"))
      (is (= (get (rdf/resource->clj (.createResource model "http://www.w3.org/2001/XMLSchema#")) "@id")
             "http://www.w3.org/2001/XMLSchema#")
          "Conversion of referents"))))
