PREFIX cssz-dimension: <https://data.cssz.cz/ontology/dimension/>
PREFIX owl:            <http://www.w3.org/2002/07/owl#>
PREFIX qb:             <http://purl.org/linked-data/cube#>

INSERT {
  GRAPH <http://ruian.linked.opendata.cz/resource/dataset> {
    ?sameAs ?p ?o .
  }
}
WHERE {
  {
    SELECT DISTINCT ?sameAs
    WHERE {
      GRAPH <https://data.cssz.cz/resource/dataset/duchodci-v-cr-krajich-okresech> {
        [] a qb:Observation ;
          cssz-dimension:refArea ?refArea ;
      }
      GRAPH <https://data.cssz.cz/resource/dataset/pomocne-ciselniky> {
        ?refArea owl:sameAs ?sameAs . 
      }
    }
  }
  SERVICE <http://ruian.linked.opendata.cz:8890/sparql> {
    GRAPH <http://ruian.linked.opendata.cz/resource/dataset> {
      ?sameAs ?p ?o .
    }
  }
}
