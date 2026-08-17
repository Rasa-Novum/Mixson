package net.rasanovum.runeweaver;

import net.rasanovum.runeweaver.enums.ErrorPolicy;
import net.rasanovum.runeweaver.enums.Lifetime;
import net.rasanovum.runeweaver.util.Index;
import net.rasanovum.runeweaver.util.functions.Event;
import net.rasanovum.runeweaver.util.interfaces.RuneweaverCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;

public class RuneweaverEventBuilder<T> {

    private RuneweaverCodec<T> codec;
    private Predicate<Index> resourcePredicate;
    private String eventName;
    private Event<T> event;
    private boolean assertive = false;
    private Lifetime lifetime;
    private ErrorPolicy errorPolicy;
    private int priority = Runeweaver.DEFAULT_PRIORITY;

    public RuneweaverEventBuilder<T> setErrorPolicy(ErrorPolicy errorPolicy) {
        this.errorPolicy = errorPolicy;
        return this;
    }

    public RuneweaverEventBuilder<T> setCodec(RuneweaverCodec<T> codec) {
        this.codec = codec;
        return this;
    }

    public RuneweaverEventBuilder<T> setResourcePredicate(Predicate<Index> resourcePredicate) {
        this.resourcePredicate = resourcePredicate;
        return this;
    }

    public RuneweaverEventBuilder<T> setEventName(String eventName) {
        this.eventName = eventName;
        return this;
    }

    public RuneweaverEventBuilder<T> setEvent(Event<T> event) {
        this.event = event;
        return this;
    }

    public RuneweaverEventBuilder<T> setAssertive(boolean assertive) {
        this.assertive = assertive;
        return this;
    }

    public RuneweaverEventBuilder<T> setLifetime(Lifetime lifetime) {
        this.lifetime = lifetime;
        return this;
    }

    public RuneweaverEventBuilder<T> setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    protected @NotNull RuneweaverEvent<T> build() {
        Objects.requireNonNull(codec, "codec must be set");
        Objects.requireNonNull(resourcePredicate, "resource predicate must be set");
        Objects.requireNonNull(event, "event must be set");
        Objects.requireNonNull(eventName, "event name must be set");
        Objects.requireNonNull(lifetime, "lifetime must be set");
        Objects.requireNonNull(errorPolicy, "error policy must be set");

        RuneweaverEvent<T> builtEvent = new RuneweaverEvent<>(codec, priority, lifetime, errorPolicy, eventName, resourcePredicate, event, assertive);
        Runeweaver.logBasic(builtEvent.getRegistrationMessage(priority));
        return builtEvent;
    }

    public RuneweaverEventBuilder<T> copy() {
        return new RuneweaverEventBuilder<T>()
                .setCodec(codec)
                .setResourcePredicate(resourcePredicate)
                .setEventName(String.valueOf(eventName))
                .setEvent(event)
                .setAssertive(assertive)
                .setErrorPolicy(errorPolicy)
                .setPriority(priority)
                .setLifetime(lifetime);
    }
}
