plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.6.0"
}

group = "org.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.4.0")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.4.0")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")


    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
    compileOnly("org.projectlombok:lombok")

    runtimeOnly("io.netty:netty-resolver-dns-native-macos:4.1.121.Final:osx-aarch_64")

    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$projectDir/src/main/resources/static/openapi/individuals-api.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/sources/openapi").get().asFile.absolutePath)
    apiPackage.set("org.example.individualsapi.api")
    modelPackage.set("org.example.individualsapi.model.dto")
    configOptions.set(mapOf(
        "interfaceOnly" to "true",
        "skipDefaultInterface" to "true",
        "useJakartaEe" to "true",
        "reactive" to "true"
    ))
}

sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated/sources/openapi/src/main/java"))
        }
    }
}

tasks.compileJava {
    dependsOn("openApiGenerate")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
