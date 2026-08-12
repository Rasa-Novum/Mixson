# Mixson Rosetta asset synchronization

`mixson-rosetta-${minecraft}-${loader}` is an optional companion artifact. It keeps Mixson's core artifact independent of Rosetta while providing a small, explicit server-owned datapack asset API.

A consumer registers each channel during common initialization. The channel discovers matching datapack files and exposes their raw bytes to a client snapshot consumer:

```java
AssetChannel.register(
        RegistryCompat.getLocation("example_mod:names"),
        "names", ".json",
        64 * 1024, 512 * 1024, 256,
        (channel, snapshot) -> NamesClient.replaceSnapshot(snapshot)
);
```

Resources are discovered from the server `ResourceManager` and sent as one complete raw-byte snapshot, cached per server. The snapshot is sent on player join and after every Rosetta datapack-reload callback. A reload with no matching resources sends an empty snapshot, so clients do not retain removed assets. The consumer owns format parsing; the channel only enforces the configured byte and count limits.

### Implementation

```kotlin
repositories {
    maven("https://raw.githubusercontent.com/Rasa-Novum/Mixson/maven/")
    maven("https://raw.githubusercontent.com/Rasa-Novum/Rosetta_Library/maven/")
}

dependencies {
    implementation("com.rasanovum.mixson:mixson-rosetta-1.21.1-fabric:2.2.0")
}
```

Use the matching `mixson-rosetta-${minecraft}-${loader}` artifact for the consumer's target. Its POM brings in the matching core Mixson and Rosetta target artifacts.
