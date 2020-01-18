# SPARQLab

SPARQLab is a lab for exercising the SPARQL query language. Your task in SPARQLab is to formulate SPARQL queries that produce results satisfying the given requirements. Each exercise is provided with an exemplary solution. The application is based on data on pension statistics from the Czech social security administration. The exercises focus on data exploration, such as for finding the dimensions used in the data, but also on domain-specific analyses, such as for detecting regions with the largest pay gap between male and female retirees.

## Deployment

SPARQLab can be deployed via [docker-compose](https://docs.docker.com/compose). It uses [Stardog Community Edition](http://stardog.com) as its RDF store, which can be installed via [this Docker image](https://github.com/jindrichmynarz/sparqlab-stardog).

SPARQLab is a web application based on the [Luminus](https://luminusweb.com) framework. If you need to run it on a non-root path of your web server, such as when in a web application container, you can set the path via the `APP_CONTEXT` enviroment variable in `docker-compose.yml`. Similarly, you can customize the SPARQL endpoint SPARQLab connects to via the `SPARQL_ENDPOINT` environment variable.

## Acknowledgements

SPARQLab was funded by developement grants of the University of Economics no. 6/2016 and no. 50/2017. The application received additional support from the prize for the best student application based on Czech open data in the Czech Open Data Challenge 2016.

## License

Copyright © 2016 Jindřich Mynarz, Vojtěch Svátek, and Jan Kučera

Distributed under the Eclipse Public License version 1.0.
