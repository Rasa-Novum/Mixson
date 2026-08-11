package net.ramixin.mixson.rosetta;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/** Explicit server-side resource discovery strategies. */
@FunctionalInterface
public interface AssetDiscovery {
    Map<Identifier, Resource> find(ResourceManager manager);

    static AssetDiscovery folder(String folder, Predicate<Identifier> predicate) {
        if (folder == null || folder.isBlank()) throw new IllegalArgumentException("folder cannot be blank");
        if (predicate == null) throw new IllegalArgumentException("predicate cannot be null");
        return manager -> manager.listResources(folder, predicate);
    }

    static AssetDiscovery folder(String folder, String extension, Predicate<Identifier> predicate) {
        if (extension == null || extension.isEmpty()) throw new IllegalArgumentException("extension cannot be empty");
        Objects.requireNonNull(predicate, "predicate");
        return folder(folder, id -> id.getPath().endsWith(extension) && predicate.test(id));
    }
}
