plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.15.5" apply false
    id("fabric-loom") version "1.15.5" apply false
    id("maven-publish")
}

stonecutter.active("26.1.2-fabric")

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
