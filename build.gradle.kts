plugins {
    java
    id("org.springframework.boot") version "2.4.1"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    id("org.openapi.generator") version "5.0.0"
    idea
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "Dwarf ticket (ProofIT)"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

sourceSets {
    main {
        java.srcDirs("src/main/java", "src/main/java-generated")
    }
}

idea {
    module {
        generatedSourceDirs.add(file("src/main/java-generated"))
    }
}

springBoot {
    mainClass.set("com.example.dt.DtApplication")
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
    testCompileOnly {
        extendsFrom(configurations.testAnnotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

val springfoxVersion by extra("2.9.2")
//val springCloudVersion by extra("Hoxton.SR1")
val springCloudVersion by extra("2020.0.0")

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

//    SpringFox dependencies
    implementation("io.springfox:springfox-swagger2:$springfoxVersion")
    implementation("io.springfox:springfox-swagger-ui:$springfoxVersion")

    implementation("org.openapitools:jackson-databind-nullable:0.2.1")
//    <!-- Bean Validation API support -->
    implementation("javax.validation:validation-api")
    implementation("org.springframework.data:spring-data-commons")

    // Circuit breaker
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks {
    getByName<Delete>("clean") {
        delete.add("$rootDir/src/main/java-generated/com")
    }

    getByName("compileJava") {
        dependsOn(getByName("openApiGenerate"))
    }

    named<Test>("test") {
        useJUnitPlatform()

//        testLogging.setShowStandardStreams(true)
//        testLogging.setExceptionFormat(org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL)
    }
}

openApiValidate {
    inputSpec.set("$rootDir/src/main/resources/api.yaml")
}

openApiGenerate {

    verbose.set(false)
    inputSpec.set("$rootDir/src/main/resources/api.yaml")
    outputDir.set("$rootDir")
    configOptions.putAll(
            mapOf(
                    "sourceFolder" to "src/main/java-generated",
                    "interfaceOnly" to "true",
                    "dateLibrary" to "java8"
            )
    )
    generatorName.set("spring")
    modelPackage.set("com.example.dt.model")
    apiPackage.set("com.example.dt.api")
    supportingFilesConstrainedTo.add("ApiUtil.java")
    globalProperties.putAll(mapOf(
            "apis" to "",
            "models" to ""
    ))
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}")
    }
}