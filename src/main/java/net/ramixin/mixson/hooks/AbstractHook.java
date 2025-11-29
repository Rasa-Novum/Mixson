package net.ramixin.mixson.hooks;

import io.netty.util.internal.UnstableApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.util.Index;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@UnstableApi
public abstract class AbstractHook<T> {

    protected final T attachedResources;

    public AbstractHook(T attachedResources) {
        this.attachedResources = attachedResources;
    }



    public abstract Optional<List<Resource>> captureFiles(Index index);

    public abstract List<Map.Entry<Index, Resource>> getMatching(Predicate<ResourceLocation> predicate);

    public abstract void insert(Index index, List<Resource> resources, String fileExt, boolean overwrite);

    public void insert(Index index, Resource resource, String fileExt, boolean overwrite) {
        insert(index, List.of(resource), fileExt, overwrite);
    }

    public abstract void delete(Index index, String fileExt);

    public T getAttachedResources() {
        return attachedResources;
    }
}
