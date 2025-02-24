package net.ramixin.mixson.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.inline.*;
import net.ramixin.mixson.inline.entries.EventEntry;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface MixsonUtil {

    static String identifierToPathString(String resourceId, String extension) {
        String usable;
        if(resourceId.endsWith("*")) usable = removeWildcard(resourceId);
        else usable = resourceId;
        String[] splits = usable.split(":", 1);
        String namespace;
        String path;
        if(splits.length == 1) {
            namespace = "minecraft";
            path = splits[0];
        }
        else {
            namespace = splits[0];
            path = splits[1];
        }
        return namespace + '~' + path.replaceFirst("\\"+extension, "").replaceAll("/", "-");
    }

    static String stringToUsablePath(String string) {
        return string.replaceAll("[*|/\\\\:?<>\"]", "");
    }

    static <T> void addComponent(T component, int priority, UUID uuid, Map<UUID, T> components, SortedMap<Integer, List<T>> orderedComponents) {
        components.put(uuid, component);
        List<T> componentSet;
        if(orderedComponents.get(priority) == null) componentSet = new ArrayList<>();
        else componentSet = orderedComponents.get(priority);
        componentSet.add(component);
        orderedComponents.put(priority, componentSet);
    }

    static String removeWildcard(String string) {
        return string.substring(0, string.length() - 1);
    }

    @SuppressWarnings("unchecked")
    static <T> EventContext<T> createContext(ContextCreationType creationType, ResourceLocation resourceId, T file, EventEntry<T> entry, boolean markedForDeletion, Function<UUID, BuiltResourceReference<?>> referenceCallback) {
        BuiltMixsonEvent<T> event = entry.event();
        BuiltResourceReference<T>[] gatheredReferences = new BuiltResourceReference[event.referenceIds().length];
        for(int i = 0; i < event.referenceIds().length; i++) {
            BuiltResourceReference<T> ref = (BuiltResourceReference<T>) referenceCallback.apply(event.referenceIds()[i]);
            gatheredReferences[i] = ref;
        }
        return new EventContext<>(creationType, file, resourceId, entry, markedForDeletion, gatheredReferences);
    }

    static <T> Optional<T> getFile(MixsonCodec<T> codec, Resource resource, ErrorMessageProvider messageProvider, BiConsumer<Exception, ErrorMessageProvider> errorCallback) {
        try {
            return Optional.of(codec.deserialize(resource));
        } catch (IOException e) {
            errorCallback.accept(e, messageProvider);
        }
        return Optional.empty();
    }

}
