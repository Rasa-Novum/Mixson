# Mixson Rosetta asset synchronization

`mixson-rosetta-${minecraft}-${loader}` is an optional companion artifact. It keeps Mixson's core artifact independent of Rosetta while providing a small, explicit server-owned datapack asset API.

A consumer registers each channel during common initialization. The channel owns its namespace, resource discovery, decoder, limits, and client snapshot consumer:

```java
private static final AssetChannel<JsonElement> NAMES = AssetChannel
        .builder(
                RegistryCompat.getLocation("example_mod:names"),
                AssetCodecs.JSON
        )
        .discovery(AssetDiscovery.folder(
                "names",
                ".json",
                id -> id.getNamespace().equals("example_mod")
        ))
        .limits(64 * 1024, 512 * 1024, 256)
        .clientSnapshot((channel, snapshot) -> NamesClient.replaceSnapshot(snapshot))
        .register();
```

Resources are discovered from the server `ResourceManager`, decoded and re-encoded into a complete snapshot, then cached per server. The snapshot is sent on player join and after every successful Rosetta datapack-reload callback. A reload with no matching resources sends an empty snapshot, so clients do not retain removed assets.

`AssetCodecs.BYTES` is the generic byte-payload path. `AssetCodecs.JSON` is the convenience JSON path; custom codecs implement `AssetCodec<T>` when a channel needs another format. The server decodes each resource once for validation and sends the original bounded bytes. Malformed resources and assets over the per-asset, total-byte, or count limit are logged and skipped. Client snapshots are immutable and replaced as one callback value.

### Implementation

```kotlin
repositories {
    maven("https://raw.githubusercontent.com/Rasa-Novum/Mixson/maven/")
    maven("https://raw.githubusercontent.com/Rasa-Novum/Rosetta_Library/maven/")
}

dependencies {
    implementation("com.rasanovum.mixson:mixson-rosetta-26.1-fabric:2.2.0")
}
```

Use the matching `mixson-rosetta-${minecraft}-${loader}` artifact for the consumer's target. Its POM brings in the matching core Mixson and Rosetta target artifacts.
