import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar

plugins.apply("maven-publish")

val target = project.name
val minecraftVersion = target.substringBeforeLast('-')
val loader = target.substringAfterLast('-')
val isLegacyFabric = loader == "fabric" && minecraftVersion in setOf("1.20.1", "1.21.1")

group = "com.rasanovum.mixson"

val modJar = if (isLegacyFabric) {
    tasks.named<AbstractArchiveTask>("remapJar")
} else {
    tasks.named<Jar>("jar")
}

val sourcesJar = if (isLegacyFabric) {
    tasks.named<AbstractArchiveTask>("remapSourcesJar")
} else {
    tasks.named<Jar>("sourcesJar")
}

extensions.configure<PublishingExtension> {
    repositories {
        maven {
            name = "local"
            url = rootProject.layout.buildDirectory.dir("maven-repository").get().asFile.toURI()
        }
    }
    publications {
        create<MavenPublication>("mixson") {
            groupId = project.group.toString()
            artifactId = "mixson-$target"
            version = project.version.toString()

            artifact(modJar) {
                classifier = null
            }
            artifact(sourcesJar) {
                classifier = "sources"
            }

            pom {
                name = "Mixson ($target)"
                description = "A lightweight runtime JSON resource modification library for Minecraft."
                url = "https://github.com/RasaNovum/Mixson"
                licenses {
                    license {
                        name = "The MIT License"
                        url = "https://opensource.org/license/mit"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/RasaNovum/Mixson.git"
                    developerConnection = "scm:git:ssh://github.com/RasaNovum/Mixson.git"
                    url = "https://github.com/RasaNovum/Mixson"
                }
            }
        }
    }
}
