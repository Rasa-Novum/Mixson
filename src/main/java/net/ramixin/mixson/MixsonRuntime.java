package net.ramixin.mixson;

import net.ramixin.mixson.entries.AbstractEntry;
import net.ramixin.mixson.entries.EventEntry;
import net.ramixin.mixson.entries.ReferenceEntry;

import java.util.*;

public class MixsonRuntime {

    private final List<AbstractEntry> queuedEvents = new ArrayList<>();

    protected MixsonRuntime(SortedMap<Integer, List<MixsonEvent<?>>> events, SortedMap<Integer, List<ResourceReference<?>>> references) {
        TreeMap<Integer, List<AbstractEntry>> combinedEntries = new TreeMap<>();
        for(int priority : references.keySet()) {
            List<ResourceReference<?>> builtReference = references.get(priority);
            List<AbstractEntry> entries = combinedEntries.computeIfAbsent(priority, k -> new ArrayList<>());
            builtReference.stream().map((reference) -> new ReferenceEntry<>(priority, reference)).forEach(entries::add);
        }
        for(int priority : events.keySet()) {
            List<MixsonEvent<?>> builtEvents = events.get(priority);
            List<AbstractEntry> entries = combinedEntries.computeIfAbsent(priority, k -> new ArrayList<>());
            builtEvents.stream().map((event) -> new EventEntry<>(priority, event)).forEach(entries::add);
        }
        combinedEntries.sequencedValues().forEach(queuedEvents::addAll);
    }

    protected AbstractEntry pop() {
        return queuedEvents.removeFirst();
    }

    protected boolean isRunning() {
        return !queuedEvents.isEmpty();
    }

    protected void insertEntry(AbstractEntry entry) {
        int priority = entry.priority();
        for (int i = 0; i < queuedEvents.size(); i++) {
            AbstractEntry abstractEntry = queuedEvents.get(i);
            if(abstractEntry.priority() <= priority) continue;
            queuedEvents.add(i, entry);
            return;
        }
        queuedEvents.add(entry);
    }

    protected void cancelEvent(UUID uuid) {
        for (int i = 0; i < queuedEvents.size(); i++) {
            AbstractEntry abstractEntry = queuedEvents.get(i);
            if(abstractEntry instanceof EventEntry<?> eventEntry) if(eventEntry.event().uuid().equals(uuid)) {
                queuedEvents.remove(i);
                return;
            }
        }
    }

}
