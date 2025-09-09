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