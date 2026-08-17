import net.neoforged.moddevgradle.legacyforge.tasks.RemapJar
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.util.Properties

plugins {
    id("net.neoforged.moddev.legacyforge")
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

legacyForge {
    version = prop("deps.forge")
    runs {
        register("client") {
            client()
        }
        register("server") {
            server()
        }
    }

    mods {
        register("runeweaver") {
            sourceSet(sourceSets.main.get())
        }
    }
}

mixin {
    add(sourceSets.main.get(), "runeweaver.refmap.json")
    config("runeweaver.forge.mixins.json")
}

repositories {
    mavenCentral()
}

dependencies {
    val mixinExtrasVersion = prop("deps.mixinextras")
    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:$mixinExtrasVersion")!!)
    jarJar(implementation("io.github.llamalad7:mixinextras-forge:$mixinExtrasVersion")!!)
    annotationProcessor("org.spongepowered:mixin:0.8.7:processor")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val mainSourceSet = sourceSets.main.get()
sourceSets.named("test") {
    compileClasspath += mainSourceSet.output + mainSourceSet.compileClasspath
    runtimeClasspath += mainSourceSet.output + mainSourceSet.runtimeClasspath
}

tasks.processResources {
    filteringCharset = "UTF-8"

    val props = mapOf(
        "version" to project.version,
        "minecraft_version" to prop("deps.minecraft"),
        "minecraft_version_range" to prop("deps.minecraft_range"),
        "loader_version" to prop("deps.forge"),
        "loader_version_range" to prop("deps.forge_range"),
        "mixin_compatibility" to prop("mixin_compatibility"),
    )

    inputs.properties(props)

    filesMatching("META-INF/mods.toml") {
        expand(props)
    }
    filesMatching("runeweaver.forge.mixins.json") {
        expand(props)
    }
    exclude("fabric.mod.json", "META-INF/neoforge.mods.toml", "runeweaver.mixins.json")
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

tasks.named<AbstractArchiveTask>("sourcesJar") {
    archiveClassifier.set("${project.name}-sources")
}

tasks.jar {
    archiveClassifier.set(project.name)
    manifest {
        attributes("MixinConfigs" to "runeweaver.forge.mixins.json")
    }
    from("LICENSE.txt") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

val reobfRuneweaverRosettaJar = tasks.register<RemapJar>("reobfRuneweaverRosettaJar") {
    archiveBaseName.set("Runeweaver-Rosetta")
    archiveClassifier.set(null as String?)
    destinationDirectory.set(layout.buildDirectory.dir("reobf"))
    input.set(provider {
        tasks.named<Jar>("runeweaverRosettaJar").get().archiveFile.get()
    })
    dependsOn("runeweaverRosettaJar")

    val baseReobf = tasks.named<RemapJar>("reobfJar").get()
    remapOperation.toolType.set(baseReobf.remapOperation.toolType)
    remapOperation.toolClasspath.from(baseReobf.remapOperation.toolClasspath)
    remapOperation.mappings.from(baseReobf.remapOperation.mappings)
    libraries.from(baseReobf.libraries)
}

apply(from = rootProject.file("gradle/runeweaver-publishing.gradle.kts"))
apply(from = rootProject.file("gradle/runeweaver-rosetta.gradle.kts"))
apply(from = rootProject.file("gradle/runeweaver-pack-metadata.gradle.kts"))
