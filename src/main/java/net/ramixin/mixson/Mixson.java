package net.ramixin.mixson;


import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.ramixin.mixson.events.AdvancedMixsonEvent;
import net.ramixin.mixson.events.MixsonEvent;
import net.ramixin.mixson.events.SimpleMixsonEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.*;

public class Mixson {

    private static final Logger LOGGER = LoggerFactory.getLogger("Mixson");
    private static final TreeMap<Integer, List<AssociatedMixsonEvent>> events = new TreeMap<>();
    private static final HashMap<UUID, BuiltResourceReference> references = new HashMap<>();
    public static final int DEFAULT_PRIORITY = 1000;

    public static void registerModificationEvent(Identifier resourceId, Identifier eventId, final SimpleMixsonEvent event) {
        registerModificationEvent(DEFAULT_PRIORITY, resourceId, eventId, event);
    }

    public static void registerModificationEvent(int priority, Identifier resourceId, Identifier eventId, final SimpleMixsonEvent event) {
        registerModificationEvent(priority, resourceId, eventId, event, false);
    }

    public static void registerModificationEvent(int priority, Identifier resourceId, Identifier eventId, final SimpleMixsonEvent event, boolean silentlyFail) {
        register(priority, resourceId, eventId, event, silentlyFail);
    }

    public static void registerModificationEvent(int priority, Identifier resourceId, Identifier eventId, final AdvancedMixsonEvent event, boolean silentlyFail, final ResourceReference... references) {
        UUID[] referenceIds = new UUID[references.length];
        for (int i = 0, referencesLength = references.length; i < referencesLength; i++) {
            ResourceReference ref = references[i];
            UUID referenceUUID = UUID.randomUUID();
            referenceIds[i] = referenceUUID;
            register(ref.priority(), ref.resourceId(), Identifier.of("mixson", "reference_event_" + eventId.getPath()), (SimpleMixsonEvent) (elem) -> {
                Mixson.references.get(referenceUUID).fulfill(elem);
                return elem;
            }, silentlyFail);
            Mixson.references.put(referenceUUID, new BuiltResourceReference(resourceId, ref.referenceId()));
        }
        register(priority, resourceId, eventId, event, silentlyFail, referenceIds);
    }

    private static void register(int priority, Identifier resourceId, Identifier eventId, MixsonEvent event, boolean silentlyFail, final UUID... referenceIds) {
        List<AssociatedMixsonEvent> eventSet;
        if(events.get(priority) == null) eventSet = new ArrayList<>();
        else eventSet = events.get(priority);
        eventSet.add(new AssociatedMixsonEvent(resourceId.withSuffixedPath(".json"), eventId, event, silentlyFail, referenceIds));
        events.put(priority, eventSet);
    }

    public static boolean removeEvent(Identifier eventId) {
        for(List<AssociatedMixsonEvent> eventSet : events.values())
            for(AssociatedMixsonEvent event : eventSet) if(event.eventId().equals(eventId)) {
                eventSet.remove(event);
                return true;
            }
        return false;
    }

    public static List<Resource> runEvents(List<Resource> original, Identifier id) {
        JsonElement[] modifiedEntries = new JsonElement[original.size()];
        for (List<AssociatedMixsonEvent> eventSet : events.values()) for (AssociatedMixsonEvent event : eventSet) {
            if(!event.resourceId().equals(id)) continue;
            for (int i = 0; i < original.size(); i++) {
                try {
                    if(modifiedEntries[i] == null) modifiedEntries[i] = JsonParser.parseReader(original.get(i).getReader());
                    JsonElement elem = modifiedEntries[i].getAsJsonObject();
                    modifiedEntries[i] = runEvent(event, elem);
                } catch (Exception e) {
                    error(e, event);
                }
            }
        }
        for (int i = 0; i < original.size(); i++) if(modifiedEntries[i] != null) {
            Resource resource = original.get(i);
            int finalI = i;
            original.set(i, new Resource(resource.getPack(), () -> new ByteArrayInputStream(modifiedEntries[finalI].toString().getBytes()), resource::getMetadata));
        }
        return original;
    }

    private static JsonElement runEvent(AssociatedMixsonEvent event, JsonElement elem) {
        if(event.referenceIds().length == 0) {
            if(event.event() instanceof SimpleMixsonEvent simpleEvent) return simpleEvent.run(elem);
            else throw new MixsonError("Events with no resource references must be of type SimpleMixsonEvent");
        } else {
            if(event.event() instanceof AdvancedMixsonEvent advancedEvent) {
                HashMap<Identifier, BuiltResourceReference> gatheredReferences = new HashMap<>();
                for(int i = 0; i < event.referenceIds().length; i++) {
                    BuiltResourceReference ref = references.get(event.referenceIds()[i]);
                    gatheredReferences.put(ref.getReferenceId(), ref);
                }
                return advancedEvent.run(elem, gatheredReferences);
            }
            else throw new MixsonError("Events with resource references must be of type AdvancedMixsonEvent");
        }
    }

    private static void error(Exception e, AssociatedMixsonEvent event) {
        String errorString = String.format("Failed to modify json file '%s' with event '%s'\n", event.resourceId(), event.eventId());
        if(event.silentlyFail()) LOGGER.error(errorString, e);
        else throw new MixsonError(errorString+e);
    }

    public static Map<Identifier, Resource> runEvents(Map<Identifier, Resource> original) {
        HashMap<Identifier, JsonElement> modifiedEntries = new HashMap<>();
        for (List<AssociatedMixsonEvent> eventSet : Mixson.events.sequencedValues()) for (AssociatedMixsonEvent event : eventSet) {
            if (!original.containsKey(event.resourceId())) continue;
            try {
                JsonElement elem = modifiedEntries.getOrDefault(event.resourceId(), JsonParser.parseReader(original.get(event.resourceId()).getReader()));
                modifiedEntries.put(event.resourceId(), runEvent(event, elem));
            } catch (Exception e) {
                error(e, event);
            }
        }
        for (Map.Entry<Identifier, JsonElement> modifiedEntry : modifiedEntries.entrySet()) {
            Resource resource = original.get(modifiedEntry.getKey());
            if(resource == null) throw new IllegalStateException("resource was removed before modifications could be applied");
            original.put(modifiedEntry.getKey(), new Resource(resource.getPack(), () -> new ByteArrayInputStream(modifiedEntry.getValue().toString().getBytes()), resource::getMetadata));
        }
        return original;
    }
}
