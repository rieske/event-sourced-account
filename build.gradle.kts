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

tasks.test {
    useJUnitPlatform {
        excludeTags("integration")
        excludeTags("e2e")
    }
    maxParallelForks = 8
}

tasks.register<Test>("integrationTest") {
    dependsOn(tasks.test)
    useJUnitPlatform {
        includeTags("integration")
    }
    maxParallelForks = 8
}

tasks.register<Test>("e2eTest") {
    dependsOn(tasks.test)
    dependsOn("integrationTest")
    dependsOn(tasks.assemble)
    useJUnitPlatform {
        includeTags("e2e")
    }
    maxParallelForks = 8
}

configurations.all {
    resolutionStrategy {
        failOnVersionConflict()
    }
}

dependencies {
    annotationProcessor("org.projectlombok:lombok:1.18.8")
    implementation("org.projectlombok:lombok:1.18.8")

    implementation("com.sparkjava:spark-core:2.9.1")

    implementation("org.msgpack:msgpack-core:0.8.17")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.8")

    implementation("org.flywaydb:flyway-core:5.2.4")

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.2.0")

    implementation("com.h2database:h2:1.4.199")
    implementation("mysql:mysql-connector-java:8.0.16")

    testAnnotationProcessor("org.projectlombok:lombok:1.18.8")
    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation("io.rest-assured:rest-assured:4.0.0")
    testImplementation("org.reflections:reflections:0.9.11")

    testImplementation("org.testcontainers:mysql:1.11.4") {
        exclude(group = "org.hamcrest", module = "hamcrest-core")
        exclude(group = "org.slf4j", module = "slf4j-api")
        exclude(group = "javax.xml.bind", module = "jaxb-api")
        exclude(group = "net.java.dev.jna", module = "jna-platform")
    }

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")

    testImplementation("com.tngtech.archunit:archunit-junit5-api:0.10.2")
    testRuntimeOnly("com.tngtech.archunit:archunit-junit5-engine:0.10.2") {
        exclude(group = "org.junit.platform", module = "junit-platform-engine")
        exclude(group = "org.junit.jupiter", module = "junit-jupiter-engine")
    }
}

val mainClass = "lt.rieske.accounts.App"

application {
    mainClassName = mainClass
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = mainClass
    }
}

tasks.wrapper {
    gradleVersion = "5.6.3"
}
