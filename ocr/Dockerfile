FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY ocr/pom.xml ocr/pom.xml
COPY share/pom.xml share/pom.xml
COPY api/pom.xml api/pom.xml

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

RUN ./mvnw dependency:go-offline -B

COPY ocr/src ocr/src
COPY share/src share/src

RUN ./mvnw package -pl ocr -am -DskipTests -B

FROM eclipse-temurin:25-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        wget \
        tesseract-ocr \
        tesseract-ocr-eng \
        poppler-utils \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/ocr/target/ocr-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]