plugins {
	java
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "local.dev"
version = "0.0.1-SNAPSHOT"
description = "LLM Bedrock integration test"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

/**
 * AWS SDK v2 BOM
 * Keeps all AWS dependencies aligned
 */
dependencyManagement {
	imports {
		mavenBom("software.amazon.awssdk:bom:2.41.5")
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")

	/* ---------------- AWS SDK v2 (Bedrock) ---------------- */

	// Bedrock Runtime (BedrockRuntimeClient)
	implementation("software.amazon.awssdk:bedrockruntime")

	// Credentials providers (Default, Profile, SSO, etc.)
	implementation("software.amazon.awssdk:auth")

	// Region enum
	implementation("software.amazon.awssdk:regions")

	// HTTP client (REQUIRED at runtime)
	implementation("software.amazon.awssdk:url-connection-client")

	/* ---------------- Kotlin ---------------- */
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	implementation("software.amazon.awssdk:lambda")

	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
