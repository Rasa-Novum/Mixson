package net.ramixin.mixson;

import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.enums.ErrorPolciy;
import net.ramixin.mixson.enums.Lifecycle;
import net.ramixin.mixson.util.functions.Event;
import net.ramixin.mixson.util.interfaces.MixsonCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class MixsonEventBuilder<T> {

    private MixsonCodec<T> codec;
    private Predicate<ResourceLocation> resourcePredicate;
    private String eventName;
    private Event<T> event;
    private boolean assertive = false;
    private Lifecycle lifecycle;
    private ErrorPolciy errorPolciy;

    public MixsonEventBuilder<T> setErrorPolicy(ErrorPolciy errorPolciy) {
        this.errorPolciy = errorPolciy;
        return this;
    }

    public MixsonEventBuilder<T> setCodec(MixsonCodec<T> codec) {
        this.codec = codec;
        return this;
    }

    public MixsonEventBuilder<T> setResourcePredicate(Predicate<ResourceLocation> resourcePredicate) {
        this.resourcePredicate = resourcePredicate;
        return this;
    }

    public MixsonEventBuilder<T> setEventName(String eventName) {
        this.eventName = eventName;
        return this;
    }

    public MixsonEventBuilder<T> setEvent(Event<T> event) {
        this.event = event;
        return this;
    }

    public MixsonEventBuilder<T> setAssertive(boolean assertive) {
        this.assertive = assertive;
        return this;
    }

    public MixsonEventBuilder<T> setLifecycle(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
        return this;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected @NotNull MixsonEvent<T> build(int priority, Optional<AtomicReference<UUID>> uuidNet) {
        Objects.requireNonNull(codec, "codec must be set");
        Objects.requireNonNull(resourcePredicate, "resource predicate must be set");
        Objects.requireNonNull(event, "event must be set");
        Objects.requireNonNull(eventName, "event name must be set");
        Objects.requireNonNull(lifecycle, "lifecycle must be set");
        Objects.requireNonNull(errorPolciy, "fail policy must be set");
        boolean fail = event.ordinal() < -1;

        MixsonEvent<T> builtEvent = new MixsonEvent<>(codec, resourcePredicate, eventName, event, errorPolciy, assertive);
        Mixson.logBasic(builtEvent.getRegistrationMessage(priority));
        uuidNet.ifPresent(net -> net.set(builtEvent.uuid()));
        if(fail) Mixson.registrationError(new MixsonError("event ordinal value must be greater than or equal to -1"), builtEvent);
        return builtEvent;
    }

    protected boolean hasDifferentCodec(MixsonCodec<T> codec) {
        return !this.codec.equals(codec);
    }

    public MixsonEventBuilder<T> copy() {
        return new MixsonEventBuilder<T>()
                .setCodec(codec)
                .setResourcePredicate(resourcePredicate)
                .setEventName(String.valueOf(eventName))
                .setEvent(event)
                .setAssertive(assertive)
                .setErrorPolicy(errorPolciy)
                .setLifecycle(lifecycle);
    }

}
