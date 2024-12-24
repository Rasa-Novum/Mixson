# Mixson

A simple lightweight library for modded Minecraft
that allows for .json resource files to be accessed and edited in-code with an event system

---

## Supported MC Versions

| MC Version    | Fabric Version  | NeoForge Version |
|---------------|-----------------|------------------|
| 1.21.4        | ✅ `v0.0.6.1`    | ✅ `v0.0.6`       |
| 1.21 - 1.21.3 | ✅ `v0.0.6.1`    | ❌  Incompatible  |
| < 1.21        | ❌  Incompatible | ❌  Incompatible  |

Note that the fourth number in the versioning X.X.X.X, if present, is loader-specific.
That means no new content was added it the new version was just a hotfix for that branch of the repo.

## Downloading the Project

This project can be installed through jitpack. First, add the following to the *first* `repositories` section in the `build.gradle`:
```gradle
repositories {
    ...
    mavenCentral()
    maven { url 'https://jitpack.io' }
    ...
}
```
After that is added,
the dependency can be added through inserting this into the `dependencies` section in the `build.gradle`:
```gradle

dependencies {
    ...
    modImplementation 'com.github.ramixin:mixson:TAG'
    ...
}
```

The `TAG` in the above section is where the specific version of Mixson will go.
Either copy the most recent version from the table above or
go to https://jitpack.io/#ramixin/mixson.

## Usage

See the wiki for indepth usage instructions and examples: https://github.com/Ramixin/Mixson/wiki
## License

This project is under an MIT