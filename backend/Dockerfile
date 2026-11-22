FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system lifeos && useradd --system --gid lifeos --home-dir /app lifeos \
    && mkdir -p /app/data && chown -R lifeos:lifeos /app
COPY --from=build --chown=lifeos:lifeos /app/target/*.jar app.jar
USER lifeos
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
