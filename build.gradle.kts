plugins {
    java
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

tasks {
    wrapper {
        gradleVersion = "5.6.3"
    }

    test {
        useJUnitPlatform {
            excludeTags("integration")
            excludeTags("e2e")
        }
        maxParallelForks = 8
    }

    register<Test>("integrationTest") {
        dependsOn(test)
        useJUnitPlatform {
            includeTags("integration")
        }
        maxParallelForks = 8
    }

    register<Test>("e2eTest") {
        dependsOn(test)
        dependsOn("integrationTest")
        dependsOn(assemble)
        useJUnitPlatform {
            includeTags("e2e")
        }
        maxParallelForks = 8
    }
}

configurations.all {
    resolutionStrategy {
        failOnVersionConflict()
    }
}

dependencies {
    annotationProcessor("org.projectlombok:lombok:1.18.10")
    implementation("org.projectlombok:lombok:1.18.10")

    implementation("com.sparkjava:spark-core:2.9.1")

    implementation("org.msgpack:msgpack-core:0.8.18")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.0")

    implementation("org.flywaydb:flyway-core:6.0.7")

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.3.0")

    implementation("com.h2database:h2:1.4.200")
    implementation("mysql:mysql-connector-java:8.0.18")

    testAnnotationProcessor("org.projectlombok:lombok:1.18.10")
    testImplementation("org.assertj:assertj-core:3.13.2")
    testImplementation("io.rest-assured:rest-assured:4.1.2")
    testImplementation("org.reflections:reflections:0.9.11")

    testImplementation("org.testcontainers:mysql:1.12.2") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")

    testImplementation("com.tngtech.archunit:archunit-junit5-api:0.11.0")
    testRuntimeOnly("com.tngtech.archunit:archunit-junit5-engine:0.11.0") {
        exclude(group = "org.junit.platform", module = "junit-platform-engine")
    }
}

application {
    mainClassName = "lt.rieske.accounts.App"
}
