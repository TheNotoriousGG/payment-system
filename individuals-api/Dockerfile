# syntax=docker/dockerfile:1.4
FROM gradle:8.14.2-jdk24 AS build

ARG NEXUS_URL
ARG NEXUS_USERNAME
ARG NEXUS_PASSWORD

ENV NEXUS_URL=${NEXUS_URL}
ENV NEXUS_USERNAME=${NEXUS_USERNAME}
ENV NEXUS_PASSWORD=${NEXUS_PASSWORD}

WORKDIR /home/gradle/project

COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle dependencies --no-daemon \
    -DNEXUS_URL="${NEXUS_URL}" \
    -DNEXUS_USERNAME="${NEXUS_USERNAME}" \
    -DNEXUS_PASSWORD="${NEXUS_PASSWORD}"

RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle --no-daemon build -x test \
    -DNEXUS_URL="${NEXUS_URL}" \
    -DNEXUS_USERNAME="${NEXUS_USERNAME}" \
    -DNEXUS_PASSWORD="${NEXUS_PASSWORD}"

FROM eclipse-temurin:24-jre

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /home/gradle/project/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8081
CMD ["java", "-jar", "app.jar"]
