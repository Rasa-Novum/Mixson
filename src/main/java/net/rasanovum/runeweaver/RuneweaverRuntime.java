package net.rasanovum.runeweaver;

import net.minecraft.resources.Identifier;
import net.rasanovum.runeweaver.entries.AbstractEntry;
import net.rasanovum.runeweaver.entries.EventEntry;
import net.rasanovum.runeweaver.entries.ReferenceEntry;
import net.rasanovum.runeweaver.hooks.AbstractHook;
import net.rasanovum.runeweaver.util.interfaces.ErrorMessageProvider;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.function.BiConsumer;

@ApiStatus.Internal
public class RuneweaverRuntime<T> {

    private final AbstractHook<T> hook;
    private final BiConsumer<String, Exception> errorCallback;
    private final List<AbstractEntry> queuedEvents = new ArrayList<>();

    protected RuneweaverRuntime(AbstractHook<T> hook, RuneweaverRegistry<RuneweaverEvent<?>> eventRegistry, RuneweaverRegistry<ResourceReference<?>> referenceRegistry, BiConsumer<String, Exception> logCallback) {
        this.hook = hook;
        this.errorCallback = logCallback;
        TreeMap<Integer, List<AbstractEntry>> combinedEntries = new TreeMap<>();
        SortedMap<Integer, List<ResourceReference<?>>> references = referenceRegistry.captureSnapshot();
        for(int priority : references.keySet()) {
            List<ResourceReference<?>> builtReference = references.get(priority);
            List<AbstractEntry> entries = combinedEntries.computeIfAbsent(priority, ignored -> new ArrayList<>());
            builtReference.stream().map((reference) -> new ReferenceEntry<>(priority, reference)).forEach(entries::add);
        }
        SortedMap<Integer, List<RuneweaverEvent<?>>> events = eventRegistry.captureSnapshot();
        for(int priority : events.keySet()) {
            List<RuneweaverEvent<?>> builtEvents = events.get(priority);
            List<AbstractEntry> entries = combinedEntries.computeIfAbsent(priority, ignored -> new ArrayList<>());
            builtEvents.stream().map((event) -> new EventEntry<>(priority, event)).forEach(entries::add);
        }
        combinedEntries.values().forEach(queuedEvents::addAll);
    }

    public AbstractHook<T> getHook() {
        return hook;
    }

    protected AbstractEntry pop() {
        return queuedEvents.remove(0);
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

    protected void error(Exception e, ErrorMessageProvider errorProvider, Identifier resourceId) {
        switch(errorProvider.getErrorPolicy()) {
            case LOG -> errorCallback.accept(errorProvider.getRuntimeErrorMessage(resourceId), e);
            case THROW -> throw new RuneweaverException(errorProvider.getRuntimeErrorMessage(resourceId), e);
        }
    }
}
