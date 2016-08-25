FROM java:8-alpine
MAINTAINER Jindřich Mynarz <mynarzjindrich@gmail.com>

ADD target/uberjar/sparqlab.jar /sparqlab/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/sparqlab/app.jar"]
