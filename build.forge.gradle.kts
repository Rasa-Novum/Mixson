import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.util.Properties

plugins {
    id("net.neoforged.moddev.legacyforge")
    id("maven-publish")
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
group = prop("maven_group")

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
        register("mixson") {
            sourceSet(sourceSets.main.get())
        }
    }
}

mixin {
    add(sourceSets.main.get(), "mixson.refmap.json")
    config("mixson.mixins.json")
}

repositories {
    mavenCentral()
}

dependencies {
    val mixinExtrasVersion = prop("deps.mixinextras")
    compileOnly("io.github.llamalad7:mixinextras-common:${prop("deps.mixinextras")}")
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
    filesMatching("mixson.mixins.json") {
        expand(props)
    }
    exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
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

tasks.jar {
    manifest {
        attributes("MixinConfigs" to "mixson.mixins.json")
    }
    from("LICENSE.txt") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = prop("archives_base_name")
            from(components["java"])
        }
    }
}
