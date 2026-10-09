import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create(
            providers.gradleProperty("platformType"),
            providers.gradleProperty("platformVersion")
        )
        bundledPlugin("com.intellij.java")

        pluginVerifier()
        zipSigner()
    }
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = "251"
            untilBuild = "262.*"
        }
    }
    buildSearchableOptions = false

    pluginVerification {
        ides {
            // latest patch of every major release within sinceBuild..untilBuild, used by ./gradlew verifyPlugin
            listOf("2025.1.7", "2025.2.6").forEach { create(IntelliJPlatformType.IntellijIdeaCommunity, it) }
            // from 2025.3 IntelliJ IDEA ships as a single unified distribution, there is no separate Community build
            listOf("2025.3.6", "2026.1.5", "2026.2.3").forEach { create(IntelliJPlatformType.IntellijIdea, it) }
        }
    }
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
