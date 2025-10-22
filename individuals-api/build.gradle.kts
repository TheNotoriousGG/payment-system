plugins {
    java
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.openApiGenerator)
}

group = "org.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.version.get().toInt())
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
    implementation(libs.bundles.springBootWeb)
    implementation(libs.bundles.springSecurity)
    implementation(libs.bundles.openApi)
    implementation(libs.bundles.micrometer)
    implementation(libs.bundles.mapstructBundle)

    compileOnly(libs.bundles.compileOnlyLibs)

    annotationProcessor(libs.lombok)
    annotationProcessor(libs.mapstruct.processor)

    runtimeOnly(libs.bundles.runtimeOnlyLibs)

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.bundles.testContainers)
    testImplementation(libs.bundles.springBootTest)
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

/*
──────────────────────────────────────────────────────
============== Resolve NEXUS credentials =============
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

if (nexusUrl.isNullOrBlank() || nexusUser.isNullOrBlank() || nexusPassword.isNullOrBlank()) {
    throw GradleException(
        "NEXUS_URL or NEXUS_USER or NEXUS_PASSWORD not set. " +
                "Please create a .env file with these properties or set environment variables."
    )
}

repositories {
    mavenCentral()
    maven {
        url = uri(nexusUrl)
        isAllowInsecureProtocol = true
        credentials {
            username = nexusUser
            password = nexusPassword
        }
    }
}