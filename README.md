<p align="center">

<img src="brand.png" alt="Mixson brand icon">

### A simple lightweight library for modded Minecraft that allows for .json resource files to be modified, created, or deleted at runtime through an event-based system.
#### This is a fork of the original [Mixson](https://modrinth.com/mod/mixson) by [Ramixin](https://github.com/Ramixin). This version has been refactored for multi-loader (Fabric, Forge, NeoForge) and multi-version (26.1.2, 1.21.1, 1.20.1) support using [Stonecutter](https://stonecutter.kikugie.dev/) for use with [our mods](https://github.com/Rasa-Novum/Via_Romana/).

---

## Support
| MC Version | Fabric Version | Forge Version | NeoForge Version | Quilt Version |
|:----------:|:--------------:|:-------------:|:----------------:|:-------------:|
|   26.1.x   |   ✅    |       ❌       |        ✅         |       ❌       |
|   1.21.1   |   ✅    |       ❌       |        ✅         |       ❌       |
|   1.20.1   |   ✅    |       ✅       |        ❌         |       ❌       |

## Usage
See the wiki for indepth usage instructions and examples: https://moddedmc.wiki/en/project/mixson/latest/docs

Check out https://github.com/Ramixin/Mixson for more information.

### Stonecutter
- For building, use `.\gradlew.bat :[version]:build`
- For version switching, use `.\gradlew.bat "Set active project to [version]"`
  - Versions: `26.1.2-fabric`/`1.21.1-fabric`/`1.20.1-fabric`
- For resetting version state to default, use `.\gradlew.bat "Reset active project"`

### Implementation

```kotlin
repositories {
    maven("https://raw.githubusercontent.com/Rasa-Novum/Mixson/maven/")
}

dependencies {
    implementation("com.rasanovum.mixson:mixson-1.21.1-fabric:2.2.0")
}
```

The optional `mixson-rosetta-${minecraft}-${loader}` companion provides explicit server-owned datapack asset snapshots over Rosetta. See [`docs/mixson-rosetta.md`](docs/mixson-rosetta.md) for registration and artifact details.

## License
This project is under an MIT
