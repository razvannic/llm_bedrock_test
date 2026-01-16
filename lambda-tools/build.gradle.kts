plugins {
    java
    id("com.gradleup.shadow") version "8.3.9"
    //It’s in the newer maintained line (GradleUp) and avoids the big 9.x behavior changes while still being modern.
}

group = "local.dev"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveClassifier.set("")
}
