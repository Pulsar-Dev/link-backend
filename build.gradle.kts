val ktorVersion: String by project
val kotlinVersion: String by project
val swaggerCodegenVersion: String by project
val logbackVersion: String by project
val exposedVersion: String by project
val mariadbVersion: String by project
val dotenvVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.9.23"
    id("io.ktor.plugin") version "2.3.9"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
}

group = "dev.lythium.pulsar"
version = "0.0.1"

application {
	mainClass.set("dev.lythium.pulsar.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
	implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
	implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
	implementation("io.ktor:ktor-server-auth:$ktorVersion")
	implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
	implementation("io.ktor:ktor-server-cors:$ktorVersion")
	implementation("io.ktor:ktor-server-rate-limit:$ktorVersion")
	implementation("io.ktor:ktor-server-resources:$ktorVersion")
	implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
	implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
	implementation("io.ktor:ktor-server-call-logging:$ktorVersion")


	implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
	implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
	implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

	implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")
	implementation("ch.qos.logback:logback-classic:$logbackVersion")
	implementation("org.mariadb.jdbc:mariadb-java-client:$mariadbVersion")
	implementation("io.github.cdimascio:dotenv-kotlin:$dotenvVersion")
	implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

ktor {
	fatJar {
		archiveFileName.set("pulsar-backend.jar")
	}
}
