import org.gradle.language.jvm.tasks.ProcessResources

val minecraftVersion = project.findProperty("deps.minecraft")?.toString()
    ?: error("Missing property 'deps.minecraft'")

val packMetadata = when (minecraftVersion) {
    "1.20.1" -> "\"pack_format\": 15,"
    "1.21.1" -> "\"pack_format\": 34,"
    "26.1", "26.1.2" -> "\"min_format\": 84,\n    \"max_format\": 84,"
    else -> error("Unsupported Minecraft resource-pack version: $minecraftVersion")
}

val properties = mapOf("pack_metadata" to packMetadata)

tasks.withType<ProcessResources>().configureEach {
    filteringCharset = "UTF-8"
    inputs.properties(properties)
    filesMatching("pack.mcmeta") {
        expand(properties)
    }
}
