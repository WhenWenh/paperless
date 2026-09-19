FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY api api
COPY share share

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

RUN ./mvnw -B -pl api -am package

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=build /app/api/target/api-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]