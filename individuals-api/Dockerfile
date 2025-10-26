FROM gradle:8.14.2-jdk24 AS build

ARG NEXUS_URL
ARG NEXUS_USERNAME
ARG NEXUS_PASSWORD

ENV NEXUS_URL=${NEXUS_URL}
ENV NEXUS_USERNAME=${NEXUS_USERNAME}
ENV NEXUS_PASSWORD=${NEXUS_PASSWORD}

WORKDIR /home/gradle/project

COPY build.gradle.kts settings.gradle.kts ./
COPY gradle/libs.versions.toml ./gradle/libs.versions.toml

RUN gradle dependencies --no-cache || true

COPY src ./src

RUN gradle --no-daemon build -x test

FROM eclipse-temurin:24-jre

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /home/gradle/project/build/libs/*.jar individuals-api.jar

EXPOSE 8081

CMD ["java","-jar","individuals-api.jar"]

