#!/bin/bash
# 
# Seeds the RDF store with pension yearbook data
# 

set -e

# Download data
tmpdir=$(mktemp -dt "$0")
cd $tmpdir
curl -O https://data.cssz.cz/dump/duchodci-v-cr-krajich-okresech.trig 
curl -O https://data.cssz.cz/dump/rocenka-vocabulary.trig
curl -O https://data.cssz.cz/dump/duchodci-v-cr-krajich-okresech-metadata.trig
curl -O https://data.cssz.cz/dump/pomocne-ciselniky.trig
curl -L http://purl.org/linked-data/cube# -o cube.ttl

cd ${STARDOG_HOME}/bin

# Create SPARQLab database
./stardog-admin db create -n sparqlab

# Load data
./stardog data add sparqlab ${tmpdir}/duchodci-v-cr-krajich-okresech.trig \
                            ${tmpdir}/rocenka-vocabulary.trig \
                            ${tmpdir}/duchodci-v-cr-krajich-okresech-metadata.trig \
                            ${tmpdir}/pomocne-ciselniky.trig
./stardog data add sparqlab -g http://purl.org/linked-data/cube ${tmpdir}/cube.ttl

# Transform data
./stardog query sparqlab ${SPARQLAB_HOME}/setup/sparql/fix_https_in_pension_kinds_1.ru
./stardog query sparqlab ${SPARQLAB_HOME}/setup/sparql/fix_https_in_pension_kinds_2.ru
./stardog query sparqlab ${SPARQLAB_HOME}/setup/sparql/rewrite_genders.ru
