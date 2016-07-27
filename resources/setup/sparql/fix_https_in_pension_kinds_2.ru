PREFIX pension-kinds:  <http://data.cssz.cz/ontology/pension-kinds/>

WITH <https://data.cssz.cz/resource/dataset/pomocne-ciselniky>
DELETE {
  ?s ?p ?_pensionKind .
}
INSERT {
  ?s ?p ?pensionKind .
}
WHERE {
  {
    SELECT DISTINCT ?pensionKind ?_pensionKind
    WHERE {
      ?pensionKind a pension-kinds:PensionKind .
      BIND (IRI(REPLACE(STR(?pensionKind), "^https:", "http:")) AS ?_pensionKind)
    }
  }
  ?s ?p ?_pensionKind .
}
