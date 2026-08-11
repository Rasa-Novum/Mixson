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

The companion currently publishes alongside the existing target-specific Mixson artifacts:

```text
com.rasanovum.mixson:mixson-rosetta-1.21.1-fabric:2.2.0
com.rasanovum.mixson:mixson-rosetta-1.21.1-neoforge:2.2.0
com.rasanovum.mixson:mixson-rosetta-1.20.1-fabric:2.2.0
com.rasanovum.mixson:mixson-rosetta-1.20.1-forge:2.2.0
com.rasanovum.mixson:mixson-rosetta-26.1-fabric:2.2.0
com.rasanovum.mixson:mixson-rosetta-26.1-neoforge:2.2.0
```

The POM declares the matching `mixson-${target}` dependency and a target-specific `com.rasanovum.rosetta:rosetta-${target}` dependency at Rosetta `0.1.0`. Rosetta is still supplied through the existing local target-jar convention in the current workspace; a public Rosetta Maven repository must exist before consumers can resolve that POM dependency remotely.
