package net.ramixin.mixson.hooks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.util.Index;
import net.ramixin.mixson.util.MixsonUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StandardHook extends AbstractHook<Map<ResourceLocation, Resource>> {

    public StandardHook(Map<ResourceLocation, Resource> attachedResources) {
        super(attachedResources);
    }

    @Override
    public Optional<List<Resource>> captureFiles(@NotNull Index index) {
        if(index.ordinal() != -1)
            throw new IllegalArgumentException("Standard Hook does not support ordinal indexing");
        Resource resource = this.attachedResources.get(index.id());
        if(resource == null) return Optional.empty();
        return Optional.of(List.of(resource));
    }

    @Override
    public List<Map.Entry<Index, Resource>> getMatching(Predicate<ResourceLocation> predicate) {
        return this.attachedResources
                .entrySet()
                .stream()
                .filter((entry) -> predicate.test(entry.getKey()))
                .map(entry -> Map.entry(new Index(MixsonUtil.removeExtension(entry.getKey()), -1), entry.getValue()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void insert(@NotNull Index index, List<Resource> resources, String fileExt, boolean overwrite) {
        if(resources.isEmpty())
            throw new IllegalArgumentException("Cannot insert empty resource list");
        if(index.ordinal() != -1)
            throw new IllegalArgumentException("Standard Hook does not support ordinal indexing");
        if(this.attachedResources.put(index.id().withSuffix(fileExt), resources.getFirst()) == null && overwrite)
            throw new IllegalStateException("Cannot overwrite resource: Resource with id " + index + " does not exists");
    }

    @Override
    public void delete(Index index, String fileExt) {
        if(index.ordinal() != -1)
            throw new IllegalArgumentException("Standard Hook does not support ordinal indexing");
        this.attachedResources.remove(index.id().withSuffix(fileExt));
    }
}
