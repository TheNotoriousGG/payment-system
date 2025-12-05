# syntax=docker/dockerfile:1.4
FROM gradle:8.14.2-jdk24 AS build

ARG NEXUS_URL=http://host.docker.internal:8081/repository/maven-snapshots
ARG NEXUS_USERNAME=admin
ARG NEXUS_PASSWORD=admin

ENV NEXUS_URL=${NEXUS_URL}
ENV NEXUS_USERNAME=${NEXUS_USERNAME}
ENV NEXUS_PASSWORD=${NEXUS_PASSWORD}

WORKDIR /home/gradle/project

COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src ./src
COPY openapi ./openapi

RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle --no-daemon clean build -x test

RUN gradle publish --info

RUN ls -lh build/libs/ && \
    cp $(ls -S build/libs/person-service-*.jar | grep -v plain | grep -v api-sdk | head -1) app.jar

FROM eclipse-temurin:24-jre

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /home/gradle/project/app.jar app.jar

EXPOSE 8092
CMD ["java", "-jar", "app.jar"]