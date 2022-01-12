FROM openjdk:11.0-jre-slim

WORKDIR /usr/local/runme
COPY target/lib lib
COPY target/*.jar server.jar

ENTRYPOINT ["java", "-jar", "server.jar"]
