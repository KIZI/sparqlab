FROM clojure:lein-2.8.3
MAINTAINER Jindřich Mynarz <mynarzjindrich@gmail.com>

WORKDIR /root
COPY . ./
RUN lein uberjar

EXPOSE 3000

CMD java -server -jar target/uberjar/sparqlab.jar
