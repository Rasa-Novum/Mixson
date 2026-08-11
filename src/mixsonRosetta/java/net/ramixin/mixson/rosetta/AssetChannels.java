package net.ramixin.mixson.rosetta;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.rasanovum.rosetta.event.ServerHooks;
import net.rasanovum.rosetta.network.RosettaNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Registry and lifecycle service for explicitly registered asset channels. */
public final class AssetChannels {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mixson/Rosetta Assets");
    private static final Map<Identifier, AssetChannel<?>> CHANNELS = new ConcurrentHashMap<>();
    private static volatile boolean initialized;

    private AssetChannels() {}

    public static void initialize() {
        if (initialized) return;
        synchronized (AssetChannels.class) {
            if (initialized) return;
            RosettaNetwork.channel(MixsonRosetta.MOD_ID).clientbound(
                    "asset_snapshot_s2c", AssetSnapshotS2C.class,
                    AssetSnapshotS2C::write, AssetSnapshotS2C::new, AssetSnapshotS2C::handle
            );
            ServerHooks.register(new Hooks());
            initialized = true;
        }
    }

    public static <T> AssetChannel<T> register(AssetChannel<T> channel) {
        initialize();
        if (CHANNELS.putIfAbsent(channel.id(), channel) != null) {
            throw new IllegalArgumentException("Duplicate asset channel: " + channel.id());
        }
        return channel;
    }

    static void handleClientSnapshot(AssetSnapshotS2C packet) {
        AssetChannel<?> channel = CHANNELS.get(packet.channel());
        if (channel == null) {
            LOGGER.warn("Ignoring asset snapshot for unregistered channel {}", packet.channel());
            return;
        }
        channel.receiveClientSnapshot(packet.assets());
    }

    private static final class Hooks implements ServerHooks.Callbacks {
        @Override public void onServerStarted(MinecraftServer server) { CHANNELS.values().forEach(channel -> channel.refresh(server)); }
        @Override public void onDataPackReload(MinecraftServer server) { CHANNELS.values().forEach(channel -> channel.refresh(server)); }
        @Override public void onPlayerJoin(ServerPlayer player) { CHANNELS.values().forEach(channel -> channel.sync(player)); }
        @Override public void onServerStopping(MinecraftServer server) { CHANNELS.values().forEach(channel -> channel.clear(server)); }
    }

}
