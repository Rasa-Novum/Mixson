import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete

plugins {
    id("dev.kikugie.stonecutter")
    id("fabric-loom") version "1.15.5" apply false
    id("net.neoforged.moddev") version "2.0.141" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.141" apply false
}

stonecutter.active("26.1.2-fabric")

val releaseTargets = listOf(
    "26.1.2-fabric",
    "26.1.2-neoforge",
    "1.21.1-fabric",
    "1.21.1-neoforge",
    "1.20.1-fabric",
    "1.20.1-forge",
)

val cleanReleaseArtifacts = tasks.register<Delete>("cleanReleaseArtifacts") {
    group = "build"
    description = "Deletes the aggregated release artifact directory."
    delete(layout.buildDirectory.dir("release"))
}

val collectReleaseArtifacts = tasks.register<Copy>("collectReleaseArtifacts") {
    group = "build"
    description = "Copies all distributable target jars into build/release."

    dependsOn(cleanReleaseArtifacts)
    dependsOn(releaseTargets.map { ":$it:build" })

    val archiveBaseName = providers.gradleProperty("archives_base_name").get()
    val modVersion = providers.gradleProperty("mod_version").get()

    into(layout.buildDirectory.dir("release"))

    releaseTargets.forEach { target ->
        from(layout.projectDirectory.dir("versions/$target/build/libs")) {
            include("$archiveBaseName-$modVersion-$target.jar")
        }
    }

    doLast {
        val releaseFiles = layout.buildDirectory.dir("release").get().asFile
            .listFiles { file -> file.isFile && file.extension == "jar" }
            ?.toList()
            .orEmpty()
        check(releaseFiles.size == releaseTargets.size) {
            "Expected ${releaseTargets.size} release jars, found ${releaseFiles.size}: ${releaseFiles.joinToString { it.name }}"
        }
    }
}

tasks.register("buildReleaseArtifacts") {
    group = "build"
    description = "Builds every supported target and collects release jars in build/release."
    dependsOn(collectReleaseArtifacts)
}

stonecutter {
    parameters {
        val projectName = current.project
        val loader = projectName.substringAfterLast('-')
        constants.match(loader, "fabric", "forge", "neoforge")
        constants.put("mc_26", eval(current.version, ">=26.1"))

        val usesResourceLocation = !eval(current.version, ">=26.1")
        replacements.string {
            direction = usesResourceLocation
            replace("net.minecraft.resources.Identifier", "net.minecraft.resources.ResourceLocation")
        }
        replacements.string {
            direction = usesResourceLocation
            replace("Identifier", "ResourceLocation")
        }
    }
}
