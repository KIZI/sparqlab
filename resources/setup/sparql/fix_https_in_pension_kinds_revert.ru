PREFIX cssz-dimension: <https://data.cssz.cz/ontology/dimension/>

WITH <https://data.cssz.cz/resource/dataset/duchodci-v-cr-krajich-okresech>
DELETE {
  ?observation cssz-dimension:druh-duchodu ?_pensionKind .
}
INSERT {
  ?observation cssz-dimension:druh-duchodu ?pensionKind .
}
WHERE {
  ?observation cssz-dimension:druh-duchodu ?_pensionKind .
  BIND (IRI(REPLACE(STR(?_pensionKind), "^http:", "https:")) AS ?pensionKind)
}
