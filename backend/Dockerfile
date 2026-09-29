FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S workflow && adduser -S workflow -G workflow
WORKDIR /app
COPY --from=build --chown=workflow:workflow /app/target/workflow-1.0.0.jar app.jar
USER workflow
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
