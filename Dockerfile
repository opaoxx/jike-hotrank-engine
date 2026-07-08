ARG DOCKERHUB_PREFIX=

FROM ${DOCKERHUB_PREFIX}maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

ARG DOCKERHUB_PREFIX=

FROM ${DOCKERHUB_PREFIX}eclipse-temurin:21-jre

WORKDIR /app

RUN addgroup --system app && adduser --system --ingroup app app

ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS=""

COPY --from=build /workspace/target/jike-hotrank-engine-*.jar /app/app.jar

EXPOSE 8080

USER app

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
