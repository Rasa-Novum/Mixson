package net.ramixin.mixson;


import com.google.gson.JsonElement;
import io.netty.util.internal.UnstableApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.entries.AbstractEntry;
import net.ramixin.mixson.entries.EventEntry;
import net.ramixin.mixson.entries.ReferenceEntry;
import net.ramixin.mixson.enums.DebugOption;
import net.ramixin.mixson.enums.ErrorPolciy;
import net.ramixin.mixson.enums.Lifecycle;
import net.ramixin.mixson.hooks.AbstractHook;
import net.ramixin.mixson.util.Index;
import net.ramixin.mixson.util.QuadRecord;
import net.ramixin.mixson.util.functions.Event;
import net.ramixin.mixson.util.interfaces.ErrorMessageProvider;
import net.ramixin.mixson.util.interfaces.MixsonCodec;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static net.ramixin.mixson.util.MixsonUtil.*;

@SuppressWarnings("unused")
public final class Mixson {

    private static final Logger LOGGER = LoggerFactory.getLogger("Mixson");
    private static int debugOptionFlags = 0;
    private static final Map<UUID, MixsonEvent<?>> events = new ConcurrentHashMap<>();
    private static final SortedMap<Integer, List<MixsonEvent<?>>> orderedEvents = new ConcurrentSkipListMap<>();
    private static final Map<UUID, ResourceReference<?>> references = new ConcurrentHashMap<>();
    private static final SortedMap<Integer, List<ResourceReference<?>>> orderedReferences = new ConcurrentSkipListMap<>();
    public static final int DEFAULT_PRIORITY = 1000;

    private Mixson() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    // EVENT REGISTRATION METHODS

    public static UUID registerEvent(int priority, Lifecycle lifecycle, Predicate<ResourceLocation> resourcePredicate, String eventName, ErrorPolciy errorPolciy, Event<JsonElement> event) {
        return registerEvent(MixsonCodecs.JSON_ELEMENT, priority, lifecycle, resourcePredicate, eventName, errorPolciy, event);
    }

    public static <T> UUID registerEvent(MixsonCodec<T> codec, int priority, Lifecycle lifecycle, Predicate<ResourceLocation> resourcePredicate, String eventName, ErrorPolciy errorPolciy, Event<T> event) {
        return registerEvent(priority, new MixsonEventBuilder<T>()
                .setCodec(codec)
                .setResourcePredicate(resourcePredicate)
                .setEventName(eventName)
                .setEvent(event)
                .setLifecycle(lifecycle)
                .setErrorPolicy(errorPolciy)
        );
    }

    public static <T> UUID registerEvent(int priority, MixsonEventBuilder<T> builder) {
        MixsonEvent<T> builtEvent = builder.build(priority, Optional.empty());
        return finalizeEventRegistration(priority, builtEvent);
    }

    public static ResourceReference<JsonElement> registerReference(int priority, Index index, String referenceName) {
        return registerReference(MixsonCodecs.JSON_ELEMENT, priority, index, referenceName);
    }

    public static <T> ResourceReference<T> registerReference(MixsonCodec<T> codec, int priority, Index index, String referenceName) {
        return registerReference(priority, new ResourceReferenceBuilder<T>()
                .setCodec(codec)
                .setIndex(index)
                .setReferenceName(referenceName)
        );
    }

    public static <T> ResourceReference<T> registerReference(int priority, ResourceReferenceBuilder<T> builder) {
        ResourceReference<T> ref = builder.build(priority);
        addComponent(ref, priority, ref.getUuid(), references, orderedReferences);
        return ref;
    }

    // SEPARATED REGISTRATION

    private static <T> UUID finalizeEventRegistration(int priority, MixsonEvent<T> builtEvent) {
        addComponent(builtEvent, priority, builtEvent.uuid(), events, orderedEvents);
        return builtEvent.uuid();
    }

    // EXTERNAL RUN METHODS

    @UnstableApi
    public static <T> T processHook(AbstractHook<T> hook) {
        MixsonRuntime runtime = new MixsonRuntime(orderedEvents, orderedReferences);
        while(runtime.isRunning()) {
            AbstractEntry entry = runtime.pop();
            switch(entry) {
                case ReferenceEntry<?> referenceEntry -> handleReference(referenceEntry, hook);
                case EventEntry<?> eventEntry -> handleEvent(eventEntry, hook, runtime::cancelEvent, runtime::insertEntry);
                default -> throw new IllegalStateException("Unexpected value: " + entry);
            }
        }
        return hook.getAttachedResources();
    }

    private static <T, R> void handleReference(ReferenceEntry<R> referenceEntry, AbstractHook<T> hook) {
        ResourceReference<R> ref = referenceEntry.reference();
        if(getDebugFlag(DebugOption.PREVENT_CATCHING))
            try {
                processReference(hook, ref);
            } catch (IOException e) {
                runtimeError(e, ref, ref.getResourceId());
            }
        else {
            try {
                processReference(hook, ref);
            } catch (Exception e) {
                runtimeError(e, ref, ref.getResourceId());
            }
        }
    }

    private static <T, R> void processReference(AbstractHook<T> hook, ResourceReference<R> ref) throws IOException {
        Optional<List<Resource>> maybeResource = hook.captureFiles(ref.getIndex());
        if(maybeResource.isEmpty()) return;
        List<Resource> resource = maybeResource.get();
        if(resource.isEmpty()) return;
        if(resource.size() > 1) {
            runtimeError(new MixsonError("resource reference cannot match more than 1 resource"), ref, ref.getIndex().id());
            return;
        }
        R file = ref.getCodec().deserialize(resource.getFirst());
        ref.fulfill(file);
    }


    private static <T> void handleEvent(EventEntry<T> eventEntry, AbstractHook<?> hook, Consumer<UUID> cancelCallback, Consumer<AbstractEntry> eventRegistrationCallback) {
        MixsonEvent<T> event = eventEntry.event();
        List<Map.Entry<Index, Resource>> entries = hook.getMatching(event.getWrappedPredicate());
        if(entries.isEmpty() && eventEntry.event().assertive()) throw new MixsonError("assertion on event '%s' failed", event.eventName());
        if(entries.isEmpty()) return;
        entries.sort(Comparator
                .comparing((Map.Entry<Index, Resource> o) -> o.getKey().id())
                .thenComparingInt(o -> o.getKey().ordinal())
        );
        Set<Index> markedForDeletion = new HashSet<>();
        logExtra("begun processing event '{}'", event.eventName());
        long fileStartTime = System.nanoTime();
        for(Map.Entry<Index, Resource> entry : entries) {
            if(getDebugFlag(DebugOption.PREVENT_CATCHING))
                try {
                    processEvent(eventEntry, hook, cancelCallback, eventRegistrationCallback, entry, markedForDeletion, event);
                } catch (IOException e) {
                    runtimeError(e, event, entry.getKey().id());
                }
            else {
                try {
                    processEvent(eventEntry, hook, cancelCallback, eventRegistrationCallback, entry, markedForDeletion, event);
                } catch (Exception e) {
                    runtimeError(e, event, entry.getKey().id());
                }
            }

        }
        String ext = eventEntry.event().codec().extensionAndDot();
        for(Index deletionIndex : markedForDeletion.stream().sorted().toList())
            hook.delete(deletionIndex, ext);
        logExtra("successfully finished processing event '{}' in {}", event.eventName(), timestamp(fileStartTime));
    }

    private static <T> void processEvent(EventEntry<T> eventEntry, AbstractHook<?> hook, Consumer<UUID> cancelCallback, Consumer<AbstractEntry> eventRegistrationCallback, Map.Entry<Index, Resource> entry, Set<Index> markedForDeletion, MixsonEvent<T> event) throws IOException {
        T file = deserializeFile(event.codec(), entry.getValue(), error -> runtimeError(error, event, entry.getKey().id())).orElse(null);
        HashMap<Index, List<Mutable<T>>> capturedFiles = new HashMap<>();
        EventContext<T> context = new EventContext<>(file, entry.getKey(), eventEntry, markedForDeletion.contains(entry.getKey()), index -> {
            Index suffixedIndex = index.withSuffixedId(event.codec().extensionAndDot());
            if(overlappingIndices(capturedFiles.keySet(), suffixedIndex)) {
                runtimeError(new MixsonError("cannot capture same file twice"), event, entry.getKey().id());
                return List.of();
            }
            Optional<List<Resource>> maybeResourceList = hook.captureFiles(suffixedIndex);
            if(maybeResourceList.isEmpty()) return List.of();
            List<Resource> resourceList = maybeResourceList.get();
            List<Mutable<T>> resultList = new ArrayList<>();
            for(Resource r : resourceList) {
                T deserializedFile = deserializeFile(event.codec(), r, error -> runtimeError(error, event, entry.getKey().id())).orElse(null);
                resultList.add(new MutableObject<>(deserializedFile));
            }
            capturedFiles.put(suffixedIndex, List.copyOf(resultList));
            return resultList;
        });
        logBasic("Running '{}' on resource '{}'", event.eventName(), entry.getKey().id());
        long fileStartTime = System.nanoTime();
        event.event().runEvent(context);
        logExtra("Finished running '{}' on resource '{}' in {}", event.eventName(), entry.getKey().id(), timestamp(fileStartTime));
        T debugExport = context.getDebugExport();
        if(debugExport != null)
            exportDebugFile(event.codec(), debugExport, event.eventName(), entry.getKey().id().toString(), event.codec().extensionAndDot());
        if(context.isMarkedForDeletion()) markedForDeletion.add(entry.getKey());
        else markedForDeletion.remove(entry.getKey());
        for(UUID cancelledFuture : context.getCancelledFutures())
            cancelCallback.accept(cancelledFuture);

        List<AbstractEntry> appendable = new ArrayList<>();
        for(QuadRecord<AtomicReference<UUID>, MixsonEventBuilder<T>, Integer, Boolean> holder : context.getCreatedEvents()) {
            MixsonEvent<T> builtEvent = holder.second().build(holder.third(), Optional.of(holder.first()));
            if(holder.fourth())
                finalizeEventRegistration(holder.third(), builtEvent);
            appendable.add(new EventEntry<>(holder.third(), builtEvent));
        }
        appendable.forEach(eventRegistrationCallback);

        hook.insert(entry.getKey(), event.codec().serialize(entry.getValue(), context.getFile()), event.codec().extensionAndDot(), true);
        for(Map.Entry<Index, List<Mutable<T>>> captureEntry : capturedFiles.entrySet()) {
            List<Resource> resources = new ArrayList<>(captureEntry.getValue().size());
            for(Mutable<T> resource : captureEntry.getValue()) {
                T resourceFile = resource.getValue();
                if(resourceFile == null) {
                    runtimeError(new MixsonError("captured file with index {} cannot be null", captureEntry.getKey()), event, captureEntry.getKey().id());
                }
                resources.add(event.codec().serialize(entry.getValue(), resourceFile));
            }
            hook.insert(captureEntry.getKey(), resources, event.codec().extensionAndDot(), true);
        }
        for(Map.Entry<Index, T> createdResource : context.getCreatedResources().entrySet())
            hook.insert(createdResource.getKey(), event.codec().serialize(entry.getValue(), createdResource.getValue()), event.codec().extensionAndDot(),false);
    }

    // ERRORS

    static void registrationError(Exception e, ErrorMessageProvider errorMessageProvider) {
        if(errorMessageProvider.getErrorPolicy() != ErrorPolciy.THROW) LOGGER.error(errorMessageProvider.getRegistrationErrorMessage(), e);
        else throw new MixsonError(errorMessageProvider.getRegistrationErrorMessage()+e);
    }

    static void runtimeError(Exception e, ErrorMessageProvider errorMessageProvider, ResourceLocation resourceId) {
        if(errorMessageProvider.getErrorPolicy() != ErrorPolciy.THROW) LOGGER.error(errorMessageProvider.getRuntimeErrorMessage(resourceId), e);
        else throw new MixsonError(errorMessageProvider.getRuntimeErrorMessage(resourceId)+e);
    }

    // MISC. PUBLICS

    public static boolean remove(UUID uuid) {
        for(List<MixsonEvent<?>> eventSet : orderedEvents.values()) eventSet.removeIf(event -> event.uuid().equals(uuid));
        if(events.remove(uuid) != null) return true;
        for(List<ResourceReference<?>> referenceSet : orderedReferences.values()) referenceSet.removeIf(event -> event.getUuid().equals(uuid));
        return references.remove(uuid) != null;
    }

    public static boolean has(UUID uuid) {
        if(events.containsKey(uuid)) return true;
        return references.containsKey(uuid);
    }

    public static String getEventName(UUID uuid) {
        return events.get(uuid).eventName();
    }

    // DEBUGGING STUFF

    public static void setDebugOption(DebugOption option, boolean state, boolean overwrite) {
        if(overwrite)
            Mixson.debugOptionFlags &= ~option.getMask();
        else Mixson.debugOptionFlags |= option.getMask();
    }

    private static boolean getDebugFlag(DebugOption option) {
        return (Mixson.debugOptionFlags & option.getMask()) > 0;
    }

    static void logBasic(String action, Object... args) {
        if(getDebugFlag(DebugOption.BASIC_LOGGING)) LOGGER.info(action, args);
    }

    private static void logExtra(String action, Object... args) {
        if(getDebugFlag(DebugOption.EXTRA_LOGGING)) LOGGER.info(action, args);
    }

    private static <T> void exportDebugFile(MixsonCodec<T> codec, T resource, String eventName, String resourceId, String extension) {
        if(!getDebugFlag(DebugOption.EXPORT_PATCHED_FILE)) return;
        Path dir = FabricLoader.getInstance().getGameDir().resolve(".mixson").resolve(identifierToPathString(resourceId, extension));
        try {
            Files.createDirectories(dir);
            FileOutputStream fos = new FileOutputStream(dir.resolve(stringToUsablePath(eventName)+extension).toFile());
            fos.write(codec.export(resource).toByteArray());
            fos.close();
        } catch (IOException e) {
            LOGGER.error("failed to export debug file", e);
        }

    }

    static {

        try {
            FileUtils.deleteDirectory(FabricLoader.getInstance().getGameDir().resolve(".mixson").toFile());
        } catch (Exception e) {
            LOGGER.error("failed to delete .mixson debug directory", e);
        }

    }

}
