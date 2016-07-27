PREFIX cssz-code:      <https://data.cssz.cz/ontology/sdmx/code/>
PREFIX cssz-dimension: <https://data.cssz.cz/ontology/dimension/>
PREFIX sdmx-code:      <http://purl.org/linked-data/sdmx/2009/code#>

WITH <https://data.cssz.cz/resource/dataset/duchodci-v-cr-krajich-okresech>
DELETE {
  ?observation cssz-dimension:pohlavi ?cssz .
}
INSERT {
  ?observation cssz-dimension:pohlavi ?sdmx .
}
WHERE {
  VALUES (?cssz ?sdmx) {
    (cssz-code:sex-F sdmx-code:sex-F)
    (cssz-code:sex-M sdmx-code:sex-M)
    (cssz-code:sex-T sdmx-code:sex-T)
  }
  ?observation cssz-dimension:pohlavi ?cssz .
}
