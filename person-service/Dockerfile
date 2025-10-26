FROM gradle:8.14.2-jdk24 AS build

ARG NEXUS_URL
ARG NEXUS_USERNAME
ARG NEXUS_PASSWORD

ENV NEXUS_URL=${NEXUS_URL}
ENV NEXUS_USERNAME=${NEXUS_USERNAME}
ENV NEXUS_PASSWORD=${NEXUS_PASSWORD}

WORKDIR /home/gradle/project

COPY build.gradle.kts settings.gradle.kts ./

RUN gradle dependencies --no-daemon || true

COPY openapi ./openapi
COPY src ./src

RUN gradle --no-daemon build -x test
RUN gradle publish --info

FROM eclipse-temurin:24-jre

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /home/gradle/project/build/libs/*.jar person-service.jar

EXPOSE 8092

CMD ["java","-jar","person-service.jar"]