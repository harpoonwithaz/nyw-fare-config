FROM maven:3.9.16-eclipse-temurin-25 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25.0.4_7-jre-noble
COPY --from=build /target/nyw-fare-config-0.0.1-SNAPSHOT.jar nyw-fare-config.jar
EXPOSE 8080
ENTRYPOINT [ "java", "-jar", "nyw-fare-config.jar" ]