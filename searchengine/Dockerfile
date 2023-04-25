FROM amazoncorretto:17.0.7-alpine
ARG JAR_FILE=target/SearchEngine-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} SearchEngine.jar
ENTRYPOINT ["java","-jar","SearchEngine.jar"]
