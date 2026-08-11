package net.ramixin.mixson.rosetta;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Codecs for the common wire formats. Channels can provide any {@link AssetCodec}. */
public final class AssetCodecs {
    public static final AssetCodec<byte[]> BYTES = new AssetCodec<>() {
        @Override
        public byte[] decode(byte[] bytes) {
            return bytes.clone();
        }
    };

    public static final AssetCodec<JsonElement> JSON = new AssetCodec<>() {
        @Override
        public JsonElement decode(byte[] bytes) throws IOException {
            try {
                return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            } catch (RuntimeException exception) {
                throw new IOException("Invalid JSON", exception);
            }
        }

    };

    private AssetCodecs() {}
}
