plugins {
    java
}

group = "dev.mending"
version = "2.12.0"

repositories {
    mavenCentral()
    // Geyser / CloudburstMC artifacts
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    // "api" is the public extension API, "core" gives access to internal classes
    // (needed for session/entity access). Adjust the version to your Geyser build.
    compileOnly("org.geysermc.geyser:api:2.12.0-SNAPSHOT")
    compileOnly("org.geysermc.geyser:core:2.12.0-SNAPSHOT")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}
