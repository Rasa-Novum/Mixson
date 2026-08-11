package net.ramixin.mixson.rosetta;

import java.io.IOException;

/** Encodes one logical datapack asset for server loading and client snapshots. */
@FunctionalInterface
public interface AssetCodec<T> {
    T decode(byte[] bytes) throws IOException;
}
