FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java",   "-Djava.security.egd=file:/dev/./urandom",   "-Dreactor.netty.http.server.accessLogEnabled=true",   "-jar", "app.jar"]
