pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9"
}

rootProject.name = "Mixson"

stonecutter {
    create(rootProject) {
        version("26.1.2-fabric", "26.1.2").buildscript = "build.fabric-26.gradle.kts"
        version("1.21.1-fabric", "1.21.1").buildscript = "build.fabric-remap.gradle.kts"
        vcsVersion = "26.1.2-fabric"
    }
}
