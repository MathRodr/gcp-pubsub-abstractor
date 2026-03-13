plugins {
    `java-library`
    `maven-publish`
    id("io.spring.dependency-management") version "1.1.7"
    id("com.google.cloud.artifactregistry.gradle-plugin") version "2.2.0"
}

group = "com.pubsubabstractor"
version = "1.5.0"
description = "GCP PubSub Lib Messaging Abstractor"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

repositories {
    mavenLocal()
    mavenCentral()
}

extra["springBootVersion"] = "3.5.11"
extra["gcpBomVersion"] = "26.76.0"

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}")
        mavenBom("com.google.cloud:libraries-bom:${property("gcpBomVersion")}")
    }
}

dependencies {
    // GCP PubSub Official
    api("com.google.cloud:google-cloud-pubsub")
    // Spring Autoconfigure
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-actuator")
    // Spring Boot
    compileOnly("org.springframework.boot:spring-boot-starter")
    //Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "gcp-pubsub-abstractor"
            version = project.version.toString()

            versionMapping {
                usage("java-api") {
                    fromResolutionResult()
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
        }
    }

}