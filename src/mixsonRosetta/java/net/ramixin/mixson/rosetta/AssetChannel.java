package net.ramixin.mixson.rosetta;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.rasanovum.rosetta.network.RosettaNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/** A named, explicitly owned server-to-client datapack asset channel. */
public final class AssetChannel<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mixson/Rosetta Assets");

    private final Identifier id;
    private final AssetDiscovery discovery;
    private final AssetCodec<T> codec;
    private final int maxAssetBytes;
    private final long maxTotalBytes;
    private final int maxAssets;
    private final BiConsumer<Identifier, Map<Identifier, T>> clientConsumer;
    private final Map<MinecraftServer, List<AssetSnapshotS2C.Asset>> serverSnapshots = new ConcurrentHashMap<>();
    private final AtomicReference<Map<Identifier, T>> latestClientSnapshot = new AtomicReference<>(Map.of());

    AssetChannel(Builder<T> builder) {
        id = builder.id;
        discovery = builder.discovery;
        codec = builder.codec;
        maxAssetBytes = builder.maxAssetBytes;
        maxTotalBytes = builder.maxTotalBytes;
        maxAssets = builder.maxAssets;
        clientConsumer = builder.clientConsumer;
    }

    public Identifier id() { return id; }
    public Map<Identifier, T> clientSnapshot() { return latestClientSnapshot.get(); }

    void refresh(MinecraftServer server) {
        List<AssetSnapshotS2C.Asset> snapshot = load(server.getResourceManager());
        serverSnapshots.put(server, snapshot);
        send(server, snapshot);
    }

    void sync(ServerPlayer player) {
        MinecraftServer server = server(player);
        if (server == null) return;
        List<AssetSnapshotS2C.Asset> snapshot = serverSnapshots.get(server);
        if (snapshot == null) {
            refresh(server);
            return;
        }
        RosettaNetwork.sendToPlayer(new AssetSnapshotS2C(id, snapshot), player);
    }

    void clear(MinecraftServer server) {
        serverSnapshots.remove(server);
    }

    void receiveClientSnapshot(List<AssetSnapshotS2C.Asset> assets) {
        if (assets.size() > maxAssets) {
            LOGGER.warn("Ignoring oversized asset snapshot for {}: {} assets", id, assets.size());
            return;
        }
        long totalBytes = 0;
        Map<Identifier, T> decoded = new LinkedHashMap<>();
        for (AssetSnapshotS2C.Asset asset : assets) {
            byte[] bytes = asset.bytes();
            if (bytes.length > maxAssetBytes || totalBytes + bytes.length > maxTotalBytes) {
                LOGGER.warn("Skipping oversized asset {} in channel {}", asset.id(), id);
                continue;
            }
            try {
                T value = codec.decode(bytes);
                if (value != null) decoded.put(asset.id(), value);
                totalBytes += bytes.length;
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("Skipping malformed asset {} in channel {}", asset.id(), id, exception);
            }
        }
        Map<Identifier, T> snapshot = Map.copyOf(decoded);
        latestClientSnapshot.set(snapshot);
        try {
            clientConsumer.accept(id, snapshot);
        } catch (RuntimeException exception) {
            LOGGER.error("Client asset consumer failed for channel {}", id, exception);
        }
    }

    private List<AssetSnapshotS2C.Asset> load(ResourceManager manager) {
        Map<Identifier, Resource> resources;
        try {
            resources = discovery.find(manager);
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to discover assets for channel {}", id, exception);
            return List.of();
        }

        List<AssetSnapshotS2C.Asset> assets = new ArrayList<>();
        long totalBytes = 0;
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            if (assets.size() >= maxAssets) {
                LOGGER.warn("Skipping asset {} in {} because the asset count limit was reached", entry.getKey(), id);
                continue;
            }
            try {
                byte[] bytes = readLimited(entry.getValue(), maxAssetBytes);
                if (codec.decode(bytes) == null) {
                    LOGGER.warn("Skipping null asset {} in channel {}", entry.getKey(), id);
                    continue;
                }
                if (totalBytes + bytes.length > maxTotalBytes) {
                    LOGGER.warn("Skipping asset {} in {} because the total payload limit was reached", entry.getKey(), id);
                    continue;
                }
                assets.add(new AssetSnapshotS2C.Asset(entry.getKey(), bytes));
                totalBytes += bytes.length;
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("Skipping malformed asset {} in channel {}", entry.getKey(), id, exception);
            }
        }
        return List.copyOf(assets);
    }

    private void send(MinecraftServer server, List<AssetSnapshotS2C.Asset> snapshot) {
        AssetSnapshotS2C packet = new AssetSnapshotS2C(id, snapshot);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            RosettaNetwork.sendToPlayer(packet, player);
        }
    }

    private static byte[] readLimited(Resource resource, int maximum) throws IOException {
        try (InputStream input = resource.open(); ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maximum, 8192))) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maximum) throw new IOException("resource exceeds " + maximum + " bytes");
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static MinecraftServer server(ServerPlayer player) {
        //? if >=26.1 {
        return player.level().getServer();
        //?} else {
        /*return player.getServer();
        *///?}
    }

    public static <T> Builder<T> builder(Identifier id, AssetCodec<T> codec) {
        return new Builder<>(id, codec);
    }

    public static final class Builder<T> {
        private final Identifier id;
        private final AssetCodec<T> codec;
        private AssetDiscovery discovery;
        private int maxAssetBytes = 1024 * 1024;
        private long maxTotalBytes = 8L * 1024 * 1024;
        private int maxAssets = 1024;
        private BiConsumer<Identifier, Map<Identifier, T>> clientConsumer;

        private Builder(Identifier id, AssetCodec<T> codec) {
            this.id = Objects.requireNonNull(id, "id");
            this.codec = Objects.requireNonNull(codec, "codec");
        }

        public Builder<T> discovery(AssetDiscovery discovery) {
            this.discovery = Objects.requireNonNull(discovery, "discovery");
            return this;
        }

        public Builder<T> limits(int maxAssetBytes, long maxTotalBytes, int maxAssets) {
            if (maxAssetBytes <= 0 || maxTotalBytes <= 0 || maxAssets <= 0) {
                throw new IllegalArgumentException("asset limits must be positive");
            }
            this.maxAssetBytes = maxAssetBytes;
            this.maxTotalBytes = maxTotalBytes;
            this.maxAssets = maxAssets;
            return this;
        }

        public Builder<T> clientSnapshot(BiConsumer<Identifier, Map<Identifier, T>> clientConsumer) {
            this.clientConsumer = Objects.requireNonNull(clientConsumer, "clientConsumer");
            return this;
        }

        public AssetChannel<T> register() {
            if (discovery == null) throw new IllegalStateException("asset discovery was not configured");
            if (clientConsumer == null) throw new IllegalStateException("client snapshot consumer was not configured");
            return AssetChannels.register(new AssetChannel<>(this));
        }
    }
}
