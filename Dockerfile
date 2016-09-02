FROM clojure
MAINTAINER Jindřich Mynarz <mynarzjindrich@gmail.com>

COPY . /
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app.jar

ADD https://data.cssz.cz/dump/duchodci-v-cr-krajich-okresech.trig /data
ADD https://data.cssz.cz/dump/rocenka-vocabulary.trig /data
ADD https://data.cssz.cz/dump/duchodci-v-cr-krajich-okresech-metadata.trig /data
ADD https://data.cssz.cz/dump/pomocne-ciselniky.trig /data
ADD http://purl.org/linked-data/cube# /data/cube.ttl
ADD /setup/sparql /setup/sparql
ADD bin/seed.sh /

EXPOSE 3000

CMD java -server -jar app.jar
