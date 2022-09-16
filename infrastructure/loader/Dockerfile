FROM openjdk:11.0-jre-slim

WORKDIR /usr/local/runme
COPY target/lib lib
# having multiple .jars (e.g., *-sources.jar) breaks this cmd
COPY target/*.jar server.jar

ENTRYPOINT java $JAVA_OPTS -jar server.jar
