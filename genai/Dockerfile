FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

COPY api/pom.xml api/pom.xml
COPY ocr/pom.xml ocr/pom.xml
COPY genai/pom.xml genai/pom.xml
COPY share/pom.xml share/pom.xml

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

RUN ./mvnw dependency:go-offline -B

COPY genai/src genai/src
COPY share/src share/src

RUN ./mvnw package -pl genai -am -DskipTests -B

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=build /app/genai/target/genai-*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/app.jar"]