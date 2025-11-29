package net.ramixin.mixson;

import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.entries.EventEntry;
import net.ramixin.mixson.enums.ErrorPolciy;
import net.ramixin.mixson.util.Index;
import net.ramixin.mixson.util.QuadRecord;
import net.ramixin.mixson.util.functions.Event;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class EventContext<T> {

    private final Mutable<T> file;
    private final Index index;
    private final EventEntry<T> entry;
    private boolean markedForDeletion;
    private final Set<UUID> cancelledFutures = new HashSet<>();
    private final HashMap<Index, T> identifiedCreatedResources = new HashMap<>();
    private final List<QuadRecord<AtomicReference<UUID>, MixsonEventBuilder<T>, Integer, Boolean>> createdEvents = new ArrayList<>();
    private final Mutable<T> debugExportObject;
    private final Function<Index, List<Mutable<T>>> captureCallback;

    public EventContext(T file, Index index, EventEntry<T> entry, boolean markedForDeletion, Function<Index, List<Mutable<T>>> captureCallback) {
        this.file = new MutableObject<>(file);
        this.debugExportObject = new MutableObject<>(file);
        this.index = index;
        this.entry = entry;
        this.markedForDeletion = markedForDeletion;
        this.captureCallback = captureCallback;
    }

    public T getFile() {
        return this.file.getValue();
    }

    public void setFile(T file) {
        this.file.setValue(file);
    }

    public Index getIndex() {
        return this.index;
    }

    public String getEventName() {
        return getEvent().eventName();
    }

    public MixsonEvent<T> getEvent() {
        return entry.event();
    }

    public UUID getEventUUID() {
        return entry.event().uuid();
    }

    public int getPriority() {
        return entry.priority();
    }

    public void markForDeletion(boolean shouldDelete) {
        this.markedForDeletion = shouldDelete;
    }

    public void createResource(Index id, T elem) {
        this.identifiedCreatedResources.put(id, elem);
    }

    protected HashMap<Index, T> getCreatedResources() {
        return this.identifiedCreatedResources;
    }

    public void cancelFutureEvent(UUID uuid) {
        this.cancelledFutures.add(uuid);
    }

    public AtomicReference<UUID> registerEvent(int priority, Predicate<ResourceLocation> resourcePredicate, String eventName, Event<T> event, ErrorPolciy errorPolciy) {
        MixsonEventBuilder<T> eventBuilder = new MixsonEventBuilder<T>()
                .setCodec(this.getEvent().codec())
                .setResourcePredicate(resourcePredicate)
                .setEventName(eventName)
                .setEvent(event)
                .setErrorPolicy(errorPolciy);
        AtomicReference<UUID> ref = new AtomicReference<>();
        createdEvents.add(new QuadRecord<>(ref, eventBuilder, priority, true));
        return ref;
    }

    public AtomicReference<UUID> registerRuntimeEvent(int priority, Predicate<ResourceLocation> resourcePredicate, String eventName, Event<T> event, ErrorPolciy errorPolciy, boolean assertive) {
        MixsonEventBuilder<T> eventBuilder = new MixsonEventBuilder<T>()
                .setCodec(this.getEvent().codec())
                .setResourcePredicate(resourcePredicate)
                .setEventName(eventName)
                .setEvent(event)
                .setErrorPolicy(errorPolciy);
        AtomicReference<UUID> ref = new AtomicReference<>();
        createdEvents.add(new QuadRecord<>(ref, eventBuilder, priority, false));
        return ref;
    }

    public AtomicReference<UUID> registerRuntimeEvent(int priority, Predicate<ResourceLocation> resourcePredicate, String eventName, Event<T> event, ErrorPolciy errorPolciy) {
        return registerRuntimeEvent(priority, resourcePredicate, eventName, event, errorPolciy, false);
    }

    public AtomicReference<UUID> registerRuntimeEvent(int priority, MixsonEventBuilder<T> eventBuilder) {
        if(eventBuilder.hasDifferentCodec(this.getEvent().codec()))
            throw new MixsonError("attempted to register runtime event with different codec than the event");
        AtomicReference<UUID> ref = new AtomicReference<>();
        createdEvents.add(new QuadRecord<>(ref, eventBuilder, priority, false));
        return ref;
    }

    public AtomicReference<UUID> registerEvent(int priority, MixsonEventBuilder<T> eventBuilder) {
        if(eventBuilder.hasDifferentCodec(this.getEvent().codec()))
            throw new MixsonError("attempted to register event with different codec than the event");
        AtomicReference<UUID> ref = new AtomicReference<>();
        createdEvents.add(new QuadRecord<>(ref, eventBuilder, priority, true));
        return ref;
    }

    public List<Mutable<T>> captureFiles(Index id) {
        return captureCallback.apply(id);
    }

    public void setDebugExport(T result) {
        this.debugExportObject.setValue(result);
    }

    public void cancelDebugExport() {
        this.debugExportObject.setValue(null);
    }

    protected T getDebugExport() {
        return this.debugExportObject.getValue();
    }

    protected List<QuadRecord<AtomicReference<UUID>, MixsonEventBuilder<T>, Integer, Boolean>> getCreatedEvents() {
        return createdEvents;
    }


    protected boolean isMarkedForDeletion() {
        return this.markedForDeletion;
    }

    protected Set<UUID> getCancelledFutures() {
        return this.cancelledFutures;
    }
}
