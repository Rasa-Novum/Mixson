package net.ramixin.mixson.hooks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.util.Index;
import net.ramixin.mixson.util.MixsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ListHook extends AbstractHook<Map<ResourceLocation, List<Resource>>> {

    public ListHook(Map<ResourceLocation, List<Resource>> attachedResources) {
        super(attachedResources);
    }

    @Override
    public Optional<List<Resource>> captureFiles(Index index) {
        List<Resource> list = this.attachedResources.get(index.id());
        if(list == null) return Optional.empty();
        if(index.ordinal() == -1) return Optional.of(list);
        return Optional.of(List.of(list.get(index.ordinal())));
    }

    @Override
    public List<Map.Entry<Index, Resource>> getMatching(Predicate<ResourceLocation> predicate) {
        Stream<Map.Entry<ResourceLocation, List<Resource>>> stream = this.attachedResources
                .entrySet()
                .stream()
                .filter((entry) -> predicate.test(entry.getKey()));
        List<Map.Entry<Index, Resource>> result = new ArrayList<>();
        for(Map.Entry<ResourceLocation, List<Resource>> entry : stream.toList()) {
            for(int i = 0; i < entry.getValue().size(); i++) {
                result.add(Map.entry(new Index(MixsonUtil.removeExtension(entry.getKey()), i), entry.getValue().get(i)));
            }
        }
        return result;
    }

    @Override
    public void insert(Index index, List<Resource> resources, String fileExt, boolean overwrite) {
        if(resources.isEmpty())
            throw new IllegalArgumentException("Cannot insert empty resource list");
        Index suffixedIndex = index.withSuffixedId(fileExt);
        if(index.ordinal() == -1) {
            if(!overwrite)
                throw new IllegalStateException("Cannot set list resource without overwrite permission");
            this.attachedResources.put(index.id().withSuffix(fileExt), resources);
            return;
        }
        if(resources.size() != 1)
            throw new IllegalArgumentException("Cannot insert resource list with index "+index);
        Resource resource = resources.getFirst();
        List<Resource> immutableList = this.attachedResources.computeIfAbsent(suffixedIndex.id(), (key) -> {
            if(overwrite)
                throw new IllegalStateException("Cannot overwrite resource: Resource with id " + suffixedIndex + " does not exists");
            return new ArrayList<>();
        });
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        List<Resource> mutableList = new ArrayList<>(immutableList);
        if(overwrite)
            mutableList.set(index.ordinal(), resource);
        else
            mutableList.add(index.ordinal(), resource);
    }

    @Override
    public void delete(Index index, String fileExt) {
        List<Resource> list = this.attachedResources.get(index.id().withSuffix(fileExt));
        if(list == null) return;
        if(index.ordinal() >= list.size())
            throw new IllegalStateException("Cannot delete resource: Resource with index " + index + " does not exists");
        if(index.ordinal() == -1)
            list.clear();
        else
            list.remove(index.ordinal());
    }
}
