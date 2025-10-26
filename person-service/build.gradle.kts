import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

val versions = mapOf(
    "mapstructVersion" to "1.5.5.Final",
    "springdocOpenapiStarterWebmvcUiVersion" to "2.5.0",
    "javaxAnnotationApiVersion" to "1.3.2",
    "javaxValidationApiVersion" to "2.0.0.Final",
    "comGoogleCodeFindbugs" to "3.0.2",
    "springCloudStarterOpenfeign" to "4.1.1",
    "javaxServletApiVersion" to "2.5",
    "logbackClassicVersion" to "1.5.18",
    "comGoogleCodeFindbugs" to "3.0.2",
    "springCloudStarterOpenfeign" to "4.1.1",
    "hibernateEnversVersion" to "6.4.4.Final",
    "testContainersVersion" to "1.19.3",
    "junitJupiterVersion" to "5.10.0",
    "feignMicrometerVersion" to "13.6"
)

plugins {
    idea
    java
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("maven-publish")
    id("org.openapi.generator") version "7.15.0"
}

group = "org.example"
version = "1.0.0-SNAPSHOT"
description = "Persons domain service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.0")
        mavenBom("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.15.0")
    }
}

configurations.all {resolutionStrategy.cacheChangingModulesFor(0, "seconds")}


dependencies {
    // SPRING
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${versions["springdocOpenapiStarterWebmvcUiVersion"]}")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:${versions["springCloudStarterOpenfeign"]}")

    // OBSERVABILITY
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.github.openfeign:feign-micrometer:${versions["feignMicrometerVersion"]}")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.micrometer:micrometer-observation")
    implementation("io.micrometer:micrometer-tracing")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")
    implementation("ch.qos.logback:logback-classic:${versions["logbackClassicVersion"]}")

    // PERSISTENCE
    implementation("org.hibernate.orm:hibernate-envers:${versions["hibernateEnversVersion"]}")
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-database-postgresql")

    // HELPERS
    compileOnly("org.projectlombok:lombok")
    compileOnly("org.mapstruct:mapstruct:${versions["mapstructVersion"]}")
    compileOnly("com.google.code.findbugs:jsr305:${versions["comGoogleCodeFindbugs"]}")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor:${versions["mapstructVersion"]}")
    implementation("javax.validation:validation-api:${versions["javaxValidationApiVersion"]}")
    implementation("javax.annotation:javax.annotation-api:${versions["javaxAnnotationApiVersion"]}")

    // TEST
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testImplementation("org.junit.jupiter:junit-jupiter:${versions["junitJupiterVersion"]}")
    testImplementation("org.testcontainers:testcontainers:${versions["testContainersVersion"]}")
    testImplementation("org.testcontainers:postgresql:${versions["testContainersVersion"]}")
    testImplementation("org.testcontainers:junit-jupiter:${versions["testContainersVersion"]}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

/*
──────────────────────────────────────────────────────
============== Api generation ==============
──────────────────────────────────────────────────────
*/

val buildDirPath: String = layout.buildDirectory.get().asFile.path
val outputDirPath: String = "${buildDirPath}/generated/openapi"
val genSrcDirPath: String = "${outputDirPath}/src/main/java"

tasks.register<GenerateTask>("generateApi") {
    generatorName.set("spring")
    inputSpec.set("$rootDir/openapi/person-api.yaml")
    outputDir.set(outputDirPath)
    configOptions.set(
        mapOf(
            "library" to "spring-cloud",
            "skipDefaultInterface" to "true",
            "useBeanValidation" to "true",
            "useFeignClientUrl" to "true",
            "openApiNullable" to "false",
            "useTags" to "true",
            "apiPackage" to "${project.group}.personapi.api",
            "modelPackage" to "${project.group}.personapi.dto",
            "configPackage" to "${project.group}.personapi.config"
        )
    )
}

sourceSets {
    main {
        java.srcDir(genSrcDirPath)
    }
}

tasks.named("compileJava") {
    dependsOn("generateApi")
}

/*
──────────────────────────────────────────────────────
============== Building jars ==============
──────────────────────────────────────────────────────
*/

val personApiSdkArtifactName = "person-api-sdk"

val generatedSourceSet = sourceSets.create("generated") {
    java.srcDir(genSrcDirPath)
    compileClasspath += sourceSets["main"].output + sourceSets["main"].compileClasspath
}

tasks.register<Jar>("generateSdkJar") {
    group = "build"
    archiveBaseName.set(personApiSdkArtifactName)
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    from(generatedSourceSet.output)
    dependsOn("generateApi")
}

/*
──────────────────────────────────────────────────────
============== Resolve NEXUS credentials ==============
──────────────────────────────────────────────────────
*/

file(".env").takeIf { it.exists() }?.readLines()?.forEach {
    val (k, v) = it.split("=", limit = 2)
    System.setProperty(k.trim(), v.trim())
    logger.lifecycle("${k.trim()}=${v.trim()}")
}

val nexusUrl = System.getenv("NEXUS_URL") ?: System.getProperty("NEXUS_URL")
val nexusUser = System.getenv("NEXUS_USERNAME") ?: System.getProperty("NEXUS_USERNAME")
val nexusPassword = System.getenv("NEXUS_PASSWORD") ?: System.getProperty("NEXUS_PASSWORD")

logger.lifecycle("Gradle nexusUrl = ${nexusUrl}")
logger.lifecycle("Gradle nexusUser = ${nexusUser}")
logger.lifecycle("Gradle nexusPassword = ${nexusPassword}")

if (nexusUrl.isNullOrBlank() || nexusUser.isNullOrBlank() || nexusPassword.isNullOrBlank()) {
    throw GradleException(
        "NEXUS details are not set. Create a .env file with correct properties: " +
                "NEXUS_URL, NEXUS_USERNAME, NEXUS_PASSWORD"
    )
}

/*
──────────────────────────────────────────────────────
============== Nexus Publishing ==============
──────────────────────────────────────────────────────
*/

publishing {
    publications {
        var jarFile = file("build/libs")
            .listFiles()
            ?.firstOrNull { it.name.contains(personApiSdkArtifactName) && (it.extension == "jar" || it.extension == "zip") }

        if (jarFile != null) {
            logger.lifecycle("publishing: ${jarFile.name}")

            create<MavenPublication>("publish${name.replaceFirstChar(Char::uppercase)}Jar") {
                artifact(jarFile)
                groupId = "${project.group}"
                artifactId = personApiSdkArtifactName
                version = "1.0.0-SNAPSHOT"

                pom {
                    this.name.set("Generated API $personApiSdkArtifactName")
                    this.description.set("OpenAPI generated code for $personApiSdkArtifactName")
                }
            }
        }
    }


    repositories {
        maven {
            name = "nexus"
            url = uri(nexusUrl)
            isAllowInsecureProtocol = true
            credentials {
                username = nexusUser
                password = nexusPassword
            }
        }
    }
}