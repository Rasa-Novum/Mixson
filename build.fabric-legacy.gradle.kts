import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.testing.logging.TestLogEvent
import net.fabricmc.loom.task.RemapJarTask
import java.util.Properties

plugins {
    id("fabric-loom")
}

val versionProperties = Properties().apply {
    val localProperties = file("gradle.properties")
    if (localProperties.isFile) {
        localProperties.inputStream().use(::load)
    }
}

fun prop(name: String): String =
    versionProperties.getProperty(name)
        ?: findProperty(name)?.toString()
        ?: rootProject.findProperty(name)?.toString()
        ?: error("Missing property '$name'")

version = prop("mod_version")

base {
    archivesName = prop("archives_base_name")
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${prop("deps.loader")}")
    testImplementation("net.fabricmc:fabric-loader-junit:${prop("deps.loader")}")
}

tasks.processResources {
    filteringCharset = "UTF-8"

    val props = mapOf(
        "version" to project.version,
        "minecraft_version" to prop("deps.minecraft"),
        "loader_version" to prop("deps.loader"),
        "mixin_compatibility" to prop("mixin_compatibility"),
    )

    inputs.properties(props)

    filesMatching("fabric.mod.json") {
        expand(props)
    }
    filesMatching("mixson.mixins.json") {
        expand(props)
    }
    exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
    }
}

val targetJavaVersion = prop("java_version").toInt()

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    withSourcesJar()
    sourceCompatibility = JavaVersion.toVersion(targetJavaVersion)
    targetCompatibility = JavaVersion.toVersion(targetJavaVersion)
}

tasks.named<AbstractArchiveTask>("remapJar") {
    archiveClassifier.set(project.name)
}

tasks.named<AbstractArchiveTask>("remapSourcesJar") {
    archiveClassifier.set("${project.name}-sources")
}

tasks.jar {
    from("LICENSE.txt") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

apply(from = rootProject.file("gradle/mixson-publishing.gradle.kts"))
apply(from = rootProject.file("gradle/mixson-rosetta.gradle.kts"))
apply(from = rootProject.file("gradle/mixson-pack-metadata.gradle.kts"))

if (project.findProperty("rosetta_jar") != null || project.findProperty("publish_mixson_rosetta") == "true") {
    val companionJar = tasks.named<AbstractArchiveTask>("mixsonRosettaJar")
    val remappedCompanion = tasks.register<RemapJarTask>("remapMixsonRosettaJar") {
        inputFile.set(companionJar.flatMap { it.archiveFile })
        archiveClassifier.set(null as String?)
        dependsOn(companionJar)
    }
    tasks.named("buildMixsonRosetta") { dependsOn(remappedCompanion) }
    extensions.configure<org.gradle.api.publish.PublishingExtension> {
        publications.named<org.gradle.api.publish.maven.MavenPublication>("mixsonRosetta") {
            artifacts.removeIf { it.file == companionJar.get().archiveFile.get().asFile }
            artifact(remappedCompanion) {
                classifier = null
            }
        }
    }
}
