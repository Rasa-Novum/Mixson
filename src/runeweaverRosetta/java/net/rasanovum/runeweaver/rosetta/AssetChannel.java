package net.rasanovum.runeweaver.rosetta;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.rasanovum.rosetta.network.RosettaNetwork;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/** A small server-owned datapack asset channel carrying raw bytes. */
public final class AssetChannel {
    private final Identifier id;
    private final String folder;
    private final String extension;
    private final int maxAssetBytes;
    private final long maxTotalBytes;
    private final int maxAssets;
    private final BiConsumer<Identifier, Map<Identifier, byte[]>> clientConsumer;
    private final Map<MinecraftServer, List<AssetSnapshotS2C.Asset>> serverSnapshots = new ConcurrentHashMap<>();

    private AssetChannel(Identifier id, String folder, String extension, int maxAssetBytes,
                         long maxTotalBytes, int maxAssets,
                         BiConsumer<Identifier, Map<Identifier, byte[]>> clientConsumer) {
        this.id = id;
        this.folder = folder;
        this.extension = extension;
        this.maxAssetBytes = maxAssetBytes;
        this.maxTotalBytes = maxTotalBytes;
        this.maxAssets = maxAssets;
        this.clientConsumer = clientConsumer;
    }

    Identifier id() {
        return id;
    }

    public static AssetChannel register(Identifier id, String folder, String extension,
                                        int maxAssetBytes, long maxTotalBytes, int maxAssets,
                                        BiConsumer<Identifier, Map<Identifier, byte[]>> clientConsumer) {
        if (folder.isBlank() || extension.isEmpty() || maxAssetBytes <= 0 || maxTotalBytes <= 0 || maxAssets <= 0) {
            throw new IllegalArgumentException("invalid asset channel configuration");
        }
        if (clientConsumer == null) throw new NullPointerException("clientConsumer");
        return AssetChannels.register(new AssetChannel(id, folder, extension, maxAssetBytes,
                maxTotalBytes, maxAssets, clientConsumer));
    }

    void refresh(MinecraftServer server) {
        List<AssetSnapshotS2C.Asset> snapshot = load(server.getResourceManager());
        serverSnapshots.put(server, snapshot);
        send(server, snapshot);
    }

    void sync(ServerPlayer player) {
        MinecraftServer server = server(player);
        if (server == null) return;
        List<AssetSnapshotS2C.Asset> snapshot = serverSnapshots.get(server);
        if (snapshot == null) refresh(server);
        else RosettaNetwork.sendToPlayer(new AssetSnapshotS2C(id, snapshot), player);
    }

    void clear(MinecraftServer server) {
        serverSnapshots.remove(server);
    }

    void receiveClientSnapshot(List<AssetSnapshotS2C.Asset> assets) {
        Map<Identifier, byte[]> snapshot = new LinkedHashMap<>();
        for (AssetSnapshotS2C.Asset asset : assets) snapshot.put(asset.id(), asset.bytes());
        clientConsumer.accept(id, Map.copyOf(snapshot));
    }

    private List<AssetSnapshotS2C.Asset> load(ResourceManager manager) {
        Map<Identifier, Resource> resources = manager.listResources(folder,
                location -> location.getPath().endsWith(extension));
        List<AssetSnapshotS2C.Asset> assets = new ArrayList<>(resources.size());
        long totalBytes = 0;
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList()) {
            if (assets.size() >= maxAssets) break;
            byte[] bytes = readLimited(entry.getValue(), maxAssetBytes);
            if (totalBytes + bytes.length > maxTotalBytes) break;
            assets.add(new AssetSnapshotS2C.Asset(entry.getKey(), bytes));
            totalBytes += bytes.length;
        }
        return List.copyOf(assets);
    }

    private void send(MinecraftServer server, List<AssetSnapshotS2C.Asset> snapshot) {
        AssetSnapshotS2C packet = new AssetSnapshotS2C(id, snapshot);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            RosettaNetwork.sendToPlayer(packet, player);
        }
    }

    private static byte[] readLimited(Resource resource, int maximum) {
        try (InputStream input = resource.open(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(new LimitedOutputStream(output, maximum));
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read datapack asset", exception);
        }
    }

    private static MinecraftServer server(ServerPlayer player) {
        //? if >=26.1 {
        return player.level().getServer();
        //?} else {
        /*return player.getServer();
        *///?}
    }

    private static final class LimitedOutputStream extends java.io.OutputStream {
        private final java.io.OutputStream delegate;
        private final int maximum;
        private int size;

        private LimitedOutputStream(java.io.OutputStream delegate, int maximum) {
            this.delegate = delegate;
            this.maximum = maximum;
        }

        @Override public void write(int value) throws IOException {
            if (size++ >= maximum) throw new IOException("datapack asset exceeds " + maximum + " bytes");
            delegate.write(value);
        }

        @Override public void write(byte[] bytes, int offset, int length) throws IOException {
            if (size + length > maximum) throw new IOException("datapack asset exceeds " + maximum + " bytes");
            delegate.write(bytes, offset, length);
            size += length;
        }
    }
}
