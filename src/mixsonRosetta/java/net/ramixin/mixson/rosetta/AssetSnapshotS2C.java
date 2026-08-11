package net.ramixin.mixson.rosetta;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.rosetta.network.RosettaPacket;

import java.util.ArrayList;
import java.util.List;

/** One complete server-owned snapshot for one registered asset channel. */
public record AssetSnapshotS2C(Identifier channel, List<Asset> assets) implements RosettaPacket {
    private static final int MAX_WIRE_ASSET_BYTES = 16 * 1024 * 1024;
    private static final int MAX_WIRE_ASSETS = 4096;
    private static final long MAX_WIRE_TOTAL_BYTES = 32L * 1024 * 1024;

    public AssetSnapshotS2C {
        assets = assets == null ? List.of() : List.copyOf(assets);
    }

    public AssetSnapshotS2C(FriendlyByteBuf buf) {
        this(buf.readResourceLocation(), readAssets(buf));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(channel);
        buf.writeVarInt(assets.size());
        for (Asset asset : assets) {
            buf.writeResourceLocation(asset.id());
            buf.writeByteArray(asset.bytes());
        }
    }

    public void handle(Level level, Player player) {
        if (level != null && level.isClientSide()) AssetChannels.handleClientSnapshot(this);
    }

    private static List<Asset> readAssets(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_WIRE_ASSETS) throw new IllegalArgumentException("Invalid asset count: " + count);
        List<Asset> assets = new ArrayList<>(count);
        long totalBytes = 0;
        for (int i = 0; i < count; i++) {
            Identifier id = buf.readResourceLocation();
            byte[] bytes = buf.readByteArray(MAX_WIRE_ASSET_BYTES);
            totalBytes += bytes.length;
            if (totalBytes > MAX_WIRE_TOTAL_BYTES) throw new IllegalArgumentException("Asset snapshot is too large");
            assets.add(new Asset(id, bytes));
        }
        return assets;
    }

    public record Asset(Identifier id, byte[] bytes) {
        public Asset {
            bytes = bytes.clone();
        }

        @Override public byte[] bytes() { return bytes.clone(); }
    }
}
