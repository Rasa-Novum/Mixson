<p align="center"><img src="https://i.imgur.com/PWRrgUL.png" alt="Runeweaver logo" width="200">

<h1 align="center">Runeweaver</h1>

A simple lightweight library for modded Minecraft that allows `.json` resource files to be modified, created, or deleted at runtime through an event-based system.

This is a fork of the original [Mixson](https://modrinth.com/mod/mixson) by [Ramixin](https://github.com/Ramixin). This version has been refactored for multi-loader (Fabric, Forge, NeoForge) and multi-version (26.1.2, 1.21.1, 1.20.1) support using [Stonecutter](https://stonecutter.kikugie.dev/) for use with [our mods](https://github.com/Rasa-Novum/Via_Romana/).

---

## Support
| MC Version | Fabric Version | Forge Version | NeoForge Version | Quilt Version |
|:----------:|:--------------:|:-------------:|:----------------:|:-------------:|
|   26.1.x   |   ✅    |       ❌       |        ✅         |       ❌       |
|   1.21.1   |   ✅    |       ❌       |        ✅         |       ❌       |
|   1.20.1   |   ✅    |       ✅       |        ❌         |       ❌       |

## Usage
See the wiki for in-depth usage instructions and examples: https://moddedmc.wiki/en/project/runeweaver/latest/docs

Check out https://github.com/Ramixin/Mixson for more information.

### Stonecutter
- For building, use `.\gradlew.bat :[version]:build`
- For version switching, use `.\gradlew.bat "Set active project to [version]"`
  - Versions: `26.1.2-fabric`/`1.21.1-fabric`/`1.20.1-fabric`
- For resetting version state to default, use `.\gradlew.bat "Reset active project"`

### Implementation

```kotlin
repositories {
    maven("https://raw.githubusercontent.com/Rasa-Novum/runeweaver/maven/")
}

dependencies {
    implementation("com.rasanovum.runeweaver:runeweaver-1.21.1-fabric:0.1.0")
}
```

The optional `runeweaver-rosetta-${minecraft}-${loader}` companion provides explicit server-owned datapack asset snapshots over Rosetta. See [`docs/runeweaver-rosetta.md`](docs/runeweaver-rosetta.md) for registration and artifact details.

## License
This project is licensed under the [MIT License](LICENSE.txt).
