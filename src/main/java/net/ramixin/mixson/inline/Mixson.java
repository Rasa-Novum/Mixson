package net.ramixin.mixson.inline;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.HexRecord;
import net.ramixin.mixson.MixsonError;
import net.ramixin.mixson.atp.MixsonAnnotationProcessor;
import net.ramixin.mixson.debug.CallCountEntry;
import net.ramixin.mixson.debug.DebugMode;
import net.ramixin.mixson.debug.MixsonCommand;
import net.ramixin.mixson.inline.entries.AbstractEntry;
import net.ramixin.mixson.inline.entries.EventEntry;
import net.ramixin.mixson.inline.entries.ReferenceEntry;
import net.ramixin.mixson.util.ErrorMessageProvider;
import net.ramixin.mixson.util.MixsonUtil;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.ramixin.mixson.util.MixsonUtil.*;

@SuppressWarnings("unused")
public final class Mixson  implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("Mixson");
    private static DebugMode debugMode = DebugMode.OFF;
    private static final Map<UUID, BuiltMixsonEvent<?>> events = Collections.synchronizedMap(new HashMap<>());
    private static final SortedMap<Integer, List<BuiltMixsonEvent<?>>> orderedEvents = Collections.synchronizedSortedMap(new TreeMap<>());
    private static final Map<UUID, CallCountEntry> callCounts = Collections.synchronizedMap(new HashMap<>());
    private static final Map<UUID, BuiltResourceReference<?>> references = Collections.synchronizedMap(new HashMap<>());
    private static final SortedMap<Integer, List<BuiltResourceReference<?>>> orderedReferences = Collections.synchronizedSortedMap(new TreeMap<>());
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final int DEFAULT_PRIORITY = 1000;
    public static final MixsonCodec<JsonElement> JSON_ELEMENT_CODEC = MixsonCodec.create(
            "json",
            r -> JsonParser.parseReader(r.openAsReader()),
            (r, x) -> new Resource(r.source(), () -> new ByteArrayInputStream(x.toString().getBytes()), r::metadata),
            json -> {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    baos.write(json.toString().getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return baos;
            }
    );

    // REGISTRATION METHODS

    public static UUID registerEvent(int priority, String resourceId, String eventName, MixsonEvent<JsonElement> event, ResourceReference... references) {
        return registerEvent(priority, resourceId, eventName, event, false, references);
    }

    public static UUID registerEvent(int priority, String resourceId, String eventName, MixsonEvent<JsonElement> event, boolean silentlyFail, ResourceReference... references) {
        return registerEvent(JSON_ELEMENT_CODEC, priority, resourceId, eventName, event, silentlyFail, references);
    }

    public static UUID registerEvent(int priority, Function<String, Boolean> resourceLocator, String eventName, MixsonEvent<JsonElement> event, boolean silentlyFail, ResourceReference... references) {
        return registerEvent(JSON_ELEMENT_CODEC, priority, resourceLocator, eventName, event, silentlyFail, references);
    }

    public static <T> UUID registerEvent(MixsonCodec<T> codec, int priority, String resourceId, String eventName, MixsonEvent<T> event, boolean silentlyFail, ResourceReference... references) {
        return registerEvent(codec, priority, getLocatorFromString(resourceId), eventName, event, silentlyFail, references);
    }

    public static <T> UUID registerEvent(MixsonCodec<T> codec, int priority, Function<String, Boolean> resourceLocator, String eventName, MixsonEvent<T> event, boolean silentlyFail, ResourceReference... references) {
        return finalizeEventRegistration(priority, buildMixsonEvent(codec, priority, resourceLocator, eventName, event, silentlyFail, references, Mixson::finalizeReferenceRegistration));
    }


    // SEPARATED REGISTRATION

    private static <T> UUID finalizeEventRegistration(int priority, BuiltMixsonEvent<T> builtEvent) {
        addComponent(builtEvent, priority, builtEvent.uuid(), events, orderedEvents);
        return builtEvent.uuid();
    }

    private static <T> void finalizeReferenceRegistration(int priority, BuiltResourceReference<T> builtReference) {
        addComponent(builtReference, priority, builtReference.getUuid(), references, orderedReferences);
    }

    private static <T> @NotNull BuiltMixsonEvent<T> buildMixsonEvent(MixsonCodec<T> codec, int priority, Function<String, Boolean> resourceLocator, String eventName, MixsonEvent<T> event, boolean silentlyFail, ResourceReference[] references, BiConsumer<Integer, BuiltResourceReference<T>> referenceCallback) {
        boolean fail = event.ordinal() < 0 && event.ordinal() != -1;
        logEventRegistration(eventName, priority);
        UUID[] referenceIds = new UUID[references.length];
        for (int i = 0, referencesLength = references.length; i < referencesLength; i++) {
            ResourceReference ref = references[i];
            BuiltResourceReference<T> builtReference = new BuiltResourceReference<>(ref, codec);
            referenceIds[i] = builtReference.getUuid();
            referenceCallback.accept(ref.priority(), builtReference);
        }

        BuiltMixsonEvent<T> builtEvent = new BuiltMixsonEvent<>(codec, resourceLocator, eventName, event, silentlyFail, referenceIds);
        if(fail) registrationError(new MixsonError("event ordinal value cannot be negative"), builtEvent);
        return builtEvent;
    }

    // INIT

    @Override
    public void onInitialize() {
        for(ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            CustomValue mixson = mod.getMetadata().getCustomValue("mixson");
            if(mixson == null) continue;
            if(!(mixson instanceof CustomValue.CvArray array)) throw new MixsonError("'mixson' field in mod '%s' is not of type array", mod.getMetadata().getId());
            for(CustomValue entry : array) {
                if(entry.getType() != CustomValue.CvType.STRING) throw new MixsonError("'mixson' field in mod '%s' contains non-string value '%s'", mod.getMetadata().getId(), entry);
                String className = entry.getAsString();
                try {
                    MixsonAnnotationProcessor.processClass(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    throw new MixsonError("class '%s' in 'mixson' field in mod '%s' does not exist", className, mod.getMetadata().getId());
                }
            }
        }

        if(!FabricLoader.getInstance().isDevelopmentEnvironment()) return;
        if(!FabricLoader.getInstance().isModLoaded("fabric")) return;
        MixsonCommand.onInitialize();
    }

    // EXTERNAL RUN METHODS

    public static Map<ResourceLocation, Resource> runStandardEvents(Map<ResourceLocation, Resource> original) {
        MixsonRuntime runtime = new MixsonRuntime(orderedEvents, orderedReferences);
        final Set<ResourceLocation> markedForDeletion = new HashSet<>();
        final Set<UUID> filledReferences = new HashSet<>();
        while(runtime.hasFinished()) {
            AbstractEntry entry = runtime.pop();
            if(entry instanceof ReferenceEntry<?> referenceEntry) {
                int ordinal = entry.getOrdinal();
                BuiltResourceReference<?> ref = referenceEntry.reference();
                ResourceLocation resourceId = ref.getResourceId().withSuffix(ref.getCodec().extensionAndDot());
                if(!original.containsKey(resourceId)) continue;
                if(ordinal >= 1) ordinalError(ordinal, 0, ref, resourceId);
                fulfillReference(original.get(resourceId), ref, filledReferences);
                continue;
            }
            EventEntry<?> eventEntry = (EventEntry<?>) entry;
            BuiltMixsonEvent<?> event = eventEntry.event();
            int fileOperations = 0;
            for(ResourceLocation resourceId : original.keySet()) {
                if(event.isNotApplicable(resourceId)) continue;
                fileOperations++;
                processStandardEvent(original, runtime, markedForDeletion, eventEntry, resourceId);
            }
            if(fileOperations != 0) incrementCallCounts(event, fileOperations);
        }
        filledReferences.forEach(uuid-> {
            if(references.containsKey(uuid))
                references.get(uuid).clear();
        });
        for(ResourceLocation id : markedForDeletion) original.remove(id);
        return original;
    }

    private static <T> void fulfillReference(Resource resource, BuiltResourceReference<T> ref, Set<UUID> filledReferences) {
        try {
            T file = ref.getCodec().deserialize(resource);
            ref.fulfill(file);
            filledReferences.add(ref.getUuid());
        } catch (IOException e) {
            runtimeError(e, ref, ref.getResourceId());
        }
    }

    public static Map<ResourceLocation, List<Resource>> runListEvents(Map<ResourceLocation, List<Resource>> original) {
        MixsonRuntime runtime = new MixsonRuntime(orderedEvents, orderedReferences);
        final Set<Pair<ResourceLocation, Integer>> markedForDeletion = new HashSet<>();
        final Set<UUID> filledReferences = new HashSet<>();
        while(runtime.hasFinished()) {
            AbstractEntry entry = runtime.pop();
            if(entry instanceof ReferenceEntry<?> referenceEntry) {
                int ordinal = entry.getOrdinal();
                BuiltResourceReference<?> ref = referenceEntry.reference();
                ResourceLocation resourceId = ref.getResourceId().withSuffix(ref.getCodec().extensionAndDot());
                if(!original.containsKey(resourceId)) continue;
                List<Resource> resources = original.get(resourceId);
                if(ordinal >= resources.size()) ordinalError(ordinal, resources.size()-1, ref, resourceId);
                Resource resource = resources.get(ordinal);
                fulfillReference(resource, ref, filledReferences);
                continue;
            }
            EventEntry<?> eventEntry = (EventEntry<?>) entry;
            BuiltMixsonEvent<?> event = eventEntry.event();
            int fileOperations = 0;
            for(ResourceLocation resourceId : original.keySet()) {
                if(event.isNotApplicable(resourceId)) continue;
                fileOperations++;
                prepareListEventProcessing(original, runtime, markedForDeletion, eventEntry, resourceId);
            }
            if(fileOperations != 0) incrementCallCounts(event, fileOperations);
        }
        filledReferences.forEach(uuid-> {
            if(references.containsKey(uuid))
                references.get(uuid).clear();
        });

        for(Pair<ResourceLocation, Integer> pairId : markedForDeletion) {
            List<Resource> resources = original.get(pairId.getFirst());
            resources.set(pairId.getSecond(), null);
        }
        for(ResourceLocation resourceId : original.keySet()) {
            List<Resource> resources = original.get(resourceId);
            resources.removeIf(Objects::isNull);
        }
        return original;
    }

    public static List<Resource> runNamespaceEvents(List<Resource> original, ResourceLocation id) {
        MixsonRuntime runtime = new MixsonRuntime(orderedEvents, orderedReferences);
        final Set<Integer> markedForDeletion = new HashSet<>();
        final Set<UUID> filledReferences = new HashSet<>();
        while(runtime.hasFinished()) {
            AbstractEntry entry = runtime.pop();
            if(entry instanceof ReferenceEntry<?> referenceEntry) {
                int ordinal = entry.getOrdinal();
                BuiltResourceReference<?> ref = referenceEntry.reference();
                if(!ref.getResourceId().withSuffix(ref.getCodec().extensionAndDot()).equals(id)) continue;
                if(ordinal >= original.size()) ordinalError(ordinal, original.size()-1, ref, ref.getResourceId());
                Resource resource = original.get(ordinal);
                fulfillReference(resource, ref, filledReferences);
                continue;
            }
            EventEntry<?> eventEntry = (EventEntry<?>) entry;
            BuiltMixsonEvent<?> event = eventEntry.event();
            int fileOperations = 0;
            if(event.isNotApplicable(id)) continue;
            int ordinal = eventEntry.getOrdinal();
            if(ordinal >= original.size()) ordinalError(ordinal, original.size()-1, event, id);
            if(ordinal == -1) for(int i = 0; i < original.size(); i++) {
                processNamespaceEvent(original, runtime, markedForDeletion, eventEntry, id, i);
                fileOperations++;
            } else {
                processNamespaceEvent(original, runtime, markedForDeletion, eventEntry, id, ordinal);
                fileOperations++;
            }
            incrementCallCounts(event, fileOperations);
        }
        filledReferences.forEach(uuid-> {
            if(references.containsKey(uuid))
                references.get(uuid).clear();
        });

        markedForDeletion.stream().sorted(Comparator.reverseOrder()).forEach(clazzInt -> original.remove((int) clazzInt));
        return original;
    }

    // INTERNAL RUN METHODS

    private static <T> void processNamespaceEvent(List<Resource> original, MixsonRuntime runtime, Set<Integer> markedForDeletion, EventEntry<T> eventEntry, ResourceLocation resourceId, int i) {
        BuiltMixsonEvent<T> event = eventEntry.event();
        Resource resource = original.get(i);
        Optional<T> file = getFile(event.codec(), resource, event, resourceId, Mixson::runtimeError);
        if (file.isEmpty()) return;
        try {
            EventContext<T> context = processContext(runtime, markedForDeletion, eventEntry, event, resourceId, i, file.get());
            for(T createdEntry : context.getIndexedCreatedResources())
                original.add(event.codec().serialize(resource, createdEntry));
            original.set(i, event.codec().serialize(resource, context.getFile()));
        } catch (Exception e) {
            runtimeError(e, event, resourceId);
        }
    }

    private static <T> void prepareListEventProcessing(Map<ResourceLocation, List<Resource>> original, MixsonRuntime runtime, Set<Pair<ResourceLocation, Integer>> markedForDeletion, EventEntry<T> eventEntry, ResourceLocation resourceId) {
        BuiltMixsonEvent<T> event = eventEntry.event();
        List<Resource> resources = original.get(resourceId);
        int ordinal = eventEntry.getOrdinal();
        if(ordinal >= resources.size()) ordinalError(ordinal, resources.size()-1, event, resourceId);
        if(ordinal == -1)
            for(int i = 0; i < resources.size(); i++)
                processListEvent(original, resourceId, resources, i, event, runtime, eventEntry, markedForDeletion);
        else
            processListEvent(original, resourceId, resources, ordinal, event, runtime, eventEntry, markedForDeletion);
    }

    private static <T> void processListEvent(Map<ResourceLocation, List<Resource>> original, ResourceLocation resourceId, List<Resource> resources, int ordinal, BuiltMixsonEvent<T> event, MixsonRuntime runtime, EventEntry<T> eventEntry, Set<Pair<ResourceLocation, Integer>> markedForDeletion) {
        Resource resource = resources.get(ordinal);
        Pair<ResourceLocation, Integer> pairedId = Pair.of(resourceId, ordinal);
        Optional<T> file = getFile(event.codec(), resources.get(ordinal), event, resourceId, Mixson::runtimeError);
        if (file.isEmpty()) return;
        try {
            EventContext<T> context = processContext(runtime, markedForDeletion, eventEntry, event, resourceId, pairedId, file.get());
            context.getCancelledFutures().forEach(runtime::cancelEvent);
            for(Map.Entry<ResourceLocation, T> createdEntry : context.getIdentifiedCreatedResources().entrySet()) {
                List<Resource> createdResources = original.computeIfAbsent(createdEntry.getKey(), (unused) -> new ArrayList<>());
                createdResources.add(event.codec().serialize(resource, createdEntry.getValue()));
            }
            resources.set(ordinal, event.codec().serialize(resource, context.getFile()));
        } catch (Exception e) {
            runtimeError(e, event, resourceId);
        }
    }


    private static <T> void processStandardEvent(Map<ResourceLocation, Resource> original, MixsonRuntime runtime, Set<ResourceLocation> markedForDeletion, EventEntry<T> eventEntry, ResourceLocation resourceId) {
        BuiltMixsonEvent<T> event = eventEntry.event();
        Resource resource = original.get(resourceId);
        Optional<T> file = getFile(event.codec(), resource, event, resourceId, Mixson::runtimeError);
        if (file.isEmpty()) return;
        try {
            EventContext<T> context = processContext(runtime, markedForDeletion, eventEntry, event, resourceId, resourceId, file.get());
            for(Map.Entry<ResourceLocation, T> createdEntry : context.getIdentifiedCreatedResources().entrySet())
                original.put(createdEntry.getKey(), event.codec().serialize(resource, createdEntry.getValue()));
            original.put(resourceId, event.codec().serialize(resource, context.getFile()));
        } catch (Exception e) {
            runtimeError(e, event, resourceId);
        }
    }

    private static <N, T> @NotNull EventContext<T> processContext(MixsonRuntime runtime, Set<N> markedForDeletion, EventEntry<T> eventEntry, BuiltMixsonEvent<T> event, ResourceLocation resourceId, N indexer, T file) {
        EventContext<T> context = MixsonUtil.createContext(ContextCreationType.IDENTIFIED, resourceId, file, eventEntry, markedForDeletion.contains(indexer), uuid -> runtime.getReference(uuid, references::get));
        logEventRun(event, resourceId);
        event.event().runEvent(context);
        exportDebugFile(event.codec().serializeOutputFile(context.getFile()), event.eventName(), resourceId.toString(), event.codec().extensionAndDot());
        if(context.isMarkedForDeletion()) markedForDeletion.add(indexer);
        else markedForDeletion.remove(indexer);
        context.getCancelledFutures().forEach(runtime::cancelEvent);
        List<AbstractEntry> appendable = new ArrayList<>();
        for(HexRecord<Integer, Function<String, Boolean>, String, MixsonEvent<T>, Boolean, ResourceReference[]> createdEvent : context.getCreatedEvents()) {
            BuiltMixsonEvent<T> builtEvent = createdEvent.apply((integer, locator, string2, mixsonEvent, aBoolean, resourceReferences) -> buildMixsonEvent(event.codec(), integer, locator, string2, mixsonEvent, aBoolean, resourceReferences, (integer1, reference) -> {
                appendable.add(new ReferenceEntry<>(integer1, reference));
                finalizeReferenceRegistration(integer1, reference);
            }));
            appendable.add(new EventEntry<>(createdEvent.first(), builtEvent));
            finalizeEventRegistration(createdEvent.first(), builtEvent);
        }
        for(HexRecord<Integer, Function<String, Boolean>, String, MixsonEvent<T>, Boolean, ResourceReference[]> createdEvent : context.getCreatedRuntimeEvents()) {
            BuiltMixsonEvent<T> builtEvent = createdEvent.apply((integer, locator, string2, mixsonEvent, aBoolean, resourceReferences) -> buildMixsonEvent(event.codec(), integer, locator, string2, mixsonEvent, aBoolean, resourceReferences, (integer1, reference) -> appendable.add(new ReferenceEntry<>(integer1, reference))));
            appendable.add(new EventEntry<>(createdEvent.first(), builtEvent));
        }
        appendable.forEach(runtime::insertEntry);
        return context;
    }

    // ERRORS

    private static void registrationError(Exception e, ErrorMessageProvider errorMessageProvider) {
        if(errorMessageProvider.failSilently()) LOGGER.error(errorMessageProvider.getRegistrationMessage(), e);
        else throw new MixsonError(errorMessageProvider.getRegistrationMessage()+e);
    }

    private static void runtimeError(Exception e, ErrorMessageProvider errorMessageProvider, ResourceLocation resourceId) {
        if(errorMessageProvider.failSilently()) LOGGER.error(errorMessageProvider.getRuntimeMessage(resourceId), e);
        else throw new MixsonError(errorMessageProvider.getRuntimeMessage(resourceId)+e);
    }

    private static void ordinalError(int ordinal, int maxOrdinal, BuiltMixsonEvent<?> event, ResourceLocation resourceId) {
        runtimeError(new MixsonError("ordinal value '"+ordinal+"' points to no value. Max Ordinal Value: "+maxOrdinal), event, resourceId);
    }

    private static void ordinalError(int ordinal, int maxOrdinal, BuiltResourceReference<?> reference, ResourceLocation resourceId) {
        runtimeError(new MixsonError("ordinal value '"+ordinal+"' points to no value. Max Ordinal Value: "+maxOrdinal), reference, resourceId);
    }

    // MISC. PUBLICS

    public static boolean removeEvent(UUID uuid) {
        for(List<BuiltMixsonEvent<?>> eventSet : orderedEvents.values()) eventSet.removeIf(event -> event.uuid().equals(uuid));
        return events.remove(uuid) != null;
    }

    public static String getEventName(UUID uuid) {
        return events.get(uuid).eventName();
    }

    // DEBUGGING STUFF

    public static void setDebugMode(DebugMode debugMode) {
        Mixson.debugMode = debugMode;
        LOGGER.info("Mixson Debug Mode has been set to: {}", debugMode);
    }

    private static void logEventRun(BuiltMixsonEvent<?> event, ResourceLocation resourceId) {
        logAction("Running '{}' on resource '{}'", event.eventName(), resourceId);
    }

    private static void logEventRegistration(String eventName, int priority) {
        logAction("Registering '{}' with priority {}", eventName, priority);
    }

    private static void logAction(String action, Object... args) {
        if(debugMode.ordinal() > 0) LOGGER.info(action, args);
    }

    private static void incrementCallCounts(BuiltMixsonEvent<?> event, int fileOperations) {
        CallCountEntry pair = callCounts.getOrDefault(event.uuid(), CallCountEntry.DEFAULT);
        callCounts.put(event.uuid(), pair.update(fileOperations));
    }

    private static void exportDebugFile(ByteArrayOutputStream stream, String eventName, String resourceId, String extension) {
        if(debugMode.ordinal() <= 1) return;
        Path dir = FabricLoader.getInstance().getGameDir().resolve(".mixson").resolve(identifierToPathString(resourceId, extension));
        try {
            Files.createDirectories(dir);
            FileOutputStream fos = new FileOutputStream(dir.resolve(stringToUsablePath(eventName)+extension).toFile());
            fos.write(stream.toByteArray());
            fos.close();
        } catch (IOException e) {
            LOGGER.error("failed to export debug file", e);
        }

    }

    public static void clearCalls() {
        callCounts.clear();
    }

    public static List<UUID> getCallCountsOrder() {
        return callCounts
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public static CallCountEntry getCallCount(UUID uuid) {
        return callCounts.getOrDefault(uuid, CallCountEntry.DEFAULT);
    }

    static {

        try {
            FileUtils.deleteDirectory(FabricLoader.getInstance().getGameDir().resolve(".mixson").toFile());
        } catch (IOException e) {
            LOGGER.error("failed to delete .mixson debug directory", e);
        }

    }

}
