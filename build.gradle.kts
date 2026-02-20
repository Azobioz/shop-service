plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("io.ktor.plugin") version "3.0.2"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktorVersion = "3.0.2"
val exposedVersion = "0.57.0"

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-server-openapi:$ktorVersion")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:6.2.1")

    // Redis
    implementation("io.lettuce:lettuce-core:6.5.1.RELEASE")


    // Flyway migrations
    implementation("org.flywaydb:flyway-core:10.10.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.10.0")

    // Kafka
    implementation("org.apache.kafka:kafka-clients:3.9.0")

    // JWT
    implementation("com.auth0:java-jwt:4.4.0")

    // BCrypt for password hashing
    implementation("org.mindrot:jbcrypt:0.4")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.15")

    // Config
    implementation("com.typesafe:config:1.4.3")

    testImplementation("com.github.docker-java:docker-java-api:3.4.1")
    testImplementation("com.github.docker-java:docker-java-transport-httpclient5:3.4.1")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.testcontainers:testcontainers:1.21.4")
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("org.testcontainers:kafka:1.21.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("io.mockk:mockk:1.13.14")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.example.ApplicationKt")
}

ktor {
    fatJar {
        archiveFileName.set("shop-api.jar")
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "com.example.ApplicationKt"
    }
}