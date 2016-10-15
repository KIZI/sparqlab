#!/bin/bash
# 
# Starts and connects the required Docker images
# TODO: Migrate this to Docker Compose.

set -e

docker network create sparqlab

###############
### Stardog ###
###############

# cd stardog directory...
docker build -t stardog .
docker run --name stardog \
           --network sparqlab \
           -d stardog

################
### SPARQLab ###
################

# cd sparqlab directory...
docker build -t sparqlab .
docker run --name sparqlab \
           -p 3000:3000 \
           --network sparqlab \
           -d \
           -e SPARQL_ENDPOINT=http://stardog:5820/sparqlab/query \
           -e APP_CONTEXT=/sparqlab \
           sparqlab

# Open shell in a running image...
# docker exec -it stardog bash
