package net.rasanovum.runeweaver.util;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.rasanovum.runeweaver.util.interfaces.RuneweaverCodec;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

@ApiStatus.Internal
public interface RuneweaverUtil {

    static String identifierToPathString(String resourceId, String extension) {
        Identifier usable = VersionUtils.id(resourceId);
        return VersionUtils.namespace(usable) + '~' + VersionUtils.path(usable).replaceFirst(String.format("\\%s", extension), "").replace("/", "-");
    }

    static String stringToUsablePath(String string) {
        return string.replaceAll("[*|/\\\\:?<>\".]", "");
    }

    static Identifier removeExtension(Identifier id) {
        String stringId = VersionUtils.path(id);
        for(int i = stringId.length()-1; i > 0; i--) if(stringId.charAt(i) == '.') return VersionUtils.id(VersionUtils.namespace(id), stringId.substring(0, i));
        return id;
    }

    static <T> Optional<T> deserializeFile(RuneweaverCodec<T> codec, Resource resource, Consumer<Exception> errorCallback) {
        try {
            return Optional.of(codec.deserialize(resource));
        } catch (IOException e) {
            errorCallback.accept(e);
        }
        return Optional.empty();
    }

    static boolean overlappingIndices(Set<Index> indices, Index index) {
        for(Index setIndex : indices) {
            if(!setIndex.id().equals(index.id())) continue;
            if(setIndex.ordinal() == index.ordinal()) return true;
            else if(setIndex.ordinal() == -1 || index.ordinal() == -1) return true;
        }
        return false;
    }

    static String timestamp(long start) {
        long nanos = System.nanoTime() - start;
        if (nanos < 1_000) return nanos + " ns";
        if (nanos < 1_000_000) return String.format("%.3f µs", nanos / 1_000.0);
        if (nanos < 1_000_000_000) return String.format("%.3f ms", nanos / 1_000_000.0);
        return String.format("%.3f s", nanos / 1_000_000_000.0);
    }
}
