import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.language.jvm.tasks.ProcessResources

val configuredRosettaPath = providers.gradleProperty("rosetta_jar")
    .map { file(it) }
    .orNull

val rosettaJarPath = configuredRosettaPath?.let { configured ->
    if (configured.isDirectory) {
        listOf(
            configured.resolve("build/release/Rosetta-0.1.0-${project.name}.jar"),
            configured.resolve("libs/rosetta/${project.name}/Rosetta-0.1.0-${project.name}.jar"),
            configured.resolve("versions/${project.name}/build/classes/java/main"),
        ).firstOrNull { it.exists() } ?: configured
    } else {
        configured
    }
}

val publishCompanion = providers.gradleProperty("publish_mixson_rosetta")
    .map(String::toBoolean)
    .orElse(false)
    .get()
val companionTarget = project.name in setOf(
    "1.20.1-fabric", "1.20.1-forge", "1.21.1-fabric",
    "1.21.1-neoforge", "26.1-fabric", "26.1-neoforge"
)
val companionEnabled = companionTarget && (rosettaJarPath != null || publishCompanion)

val buildCompanion = tasks.register("buildMixsonRosetta") {
    group = "build"
    description = "Builds the optional Mixson/Rosetta companion artifact for this target."
}

if (companionEnabled) {
    if (rosettaJarPath != null) {
        check(rosettaJarPath.exists()) { "Rosetta dependency does not exist: $rosettaJarPath" }
    } else {
        val rosettaMavenUrl = providers.gradleProperty("rosetta_maven_url")
            .orElse("https://raw.githubusercontent.com/Rasa-Novum/Rosetta_Library/maven/")
            .get()
        project.repositories.maven {
            name = "RosettaMaven"
            url = project.uri(rosettaMavenUrl)
        }
    }

    val sourceSets = extensions.getByType<SourceSetContainer>()
    val main = sourceSets.getByName("main")
    val companion = sourceSets.maybeCreate("mixsonRosetta")
    companion.compileClasspath += main.compileClasspath
    companion.compileClasspath += main.output
    companion.runtimeClasspath += main.runtimeClasspath
    companion.runtimeClasspath += main.output

    val fabricLegacy = project.name.endsWith("-fabric") && !project.name.startsWith("26.1")
    val loaderConfiguration = if (fabricLegacy) "modImplementation" else "implementation"
    val loaderCompileClasspath = if (fabricLegacy) "modCompileClasspath" else "compileClasspath"
    if (rosettaJarPath != null) {
        dependencies.add(loaderConfiguration, files(rosettaJarPath))
    } else {
        val rosettaVersion = project.findProperty("deps.rosetta")?.toString() ?: "0.1.0"
        dependencies.add(
            loaderConfiguration,
            "com.rasanovum.rosetta:rosetta-${project.name}:$rosettaVersion"
        )
    }
    companion.compileClasspath += configurations.getByName(loaderCompileClasspath)
    companion.runtimeClasspath += configurations.getByName(loaderCompileClasspath)

    val properties = mapOf(
        "version" to project.version,
        "minecraft_version" to project.findProperty("deps.minecraft").toString(),
        "loader_version" to (project.findProperty("deps.loader")
            ?: project.findProperty("deps.forge")
            ?: project.findProperty("deps.neoforge")).toString(),
        "loader_version_range" to (project.findProperty("deps.loader_range")
            ?: project.findProperty("deps.forge_range")
            ?: project.findProperty("deps.neoforge_range")
            ?: "[1,)").toString(),
        "minecraft_version_range" to (project.findProperty("deps.minecraft_range")
            ?: project.findProperty("deps.minecraft")).toString(),
    )

    tasks.named<ProcessResources>(companion.processResourcesTaskName) {
        filteringCharset = "UTF-8"
        inputs.properties(properties)
        if (project.name.endsWith("-fabric")) {
            exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml")
        } else if (project.name.endsWith("-forge")) {
            exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
        } else {
            exclude("fabric.mod.json", "META-INF/mods.toml")
        }
        filesMatching(listOf("fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml")) {
            expand(properties)
        }
    }

    val companionJar = tasks.register<Jar>("mixsonRosettaJar") {
        archiveBaseName.set("Mixson-Rosetta")
        archiveClassifier.set(null as String?)
        from(companion.output)
        dependsOn(companion.classesTaskName)
    }

    buildCompanion.configure { dependsOn(companionJar) }

    extensions.configure<PublishingExtension> {
        publications.create<MavenPublication>("mixsonRosetta") {
            groupId = "com.rasanovum.mixson"
            artifactId = "mixson-rosetta-${project.name}"
            version = project.version.toString()
            artifact(companionJar) {
                classifier = null
            }

            pom {
                name = "Mixson Rosetta (${project.name})"
                description = "Server-owned datapack asset synchronization for Mixson and Rosetta."
            url = "https://github.com/Rasa-Novum/Mixson"
                licenses {
                    license {
                        name = "The MIT License"
                        url = "https://opensource.org/license/mit"
                    }
                }
                withXml {
                    val dependencies = asNode().appendNode("dependencies")
                    fun dependency(group: String, artifact: String, version: String) {
                        val node = dependencies.appendNode("dependency")
                        node.appendNode("groupId", group)
                        node.appendNode("artifactId", artifact)
                        node.appendNode("version", version)
                    }
                    dependency("com.rasanovum.mixson", "mixson-${project.name}", project.version.toString())
                    dependency(
                        "com.rasanovum.rosetta", "rosetta-${project.name}",
                        project.findProperty("deps.rosetta")?.toString() ?: "0.1.0"
                    )
                }
            }
        }
    }
} else {
    buildCompanion.configure {
        doFirst {
            throw GradleException("Pass -Ppublish_mixson_rosetta=true or -Prosetta_jar=<target Rosetta jar> to build the Mixson Rosetta companion")
        }
    }
}
