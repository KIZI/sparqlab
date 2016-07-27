PREFIX pension-kinds:  <http://data.cssz.cz/ontology/pension-kinds/>

WITH <https://data.cssz.cz/resource/dataset/pomocne-ciselniky>
DELETE {
  ?_pensionKind ?p ?o .
}
INSERT {
  ?pensionKind ?p ?o .
}
WHERE {
  {
    SELECT DISTINCT ?_pensionKind ?pensionKind
    WHERE {
      ?_pensionKind a pension-kinds:PensionKind .
      BIND (IRI(REPLACE(STR(?_pensionKind), "^http:", "https:")) AS ?pensionKind)
    }
  }
  ?_pensionKind ?p ?o .
}
