package net.ramixin.mixson;

import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.enums.ErrorPolciy;
import net.ramixin.mixson.util.functions.Event;
import net.ramixin.mixson.util.interfaces.ErrorMessageProvider;
import net.ramixin.mixson.util.interfaces.MixsonCodec;

import java.util.UUID;
import java.util.function.Predicate;

public record MixsonEvent<T>(MixsonCodec<T> codec, UUID uuid, Predicate<ResourceLocation> resourcePredicate, String eventName, Event<T> event, ErrorPolciy errorPolciy, boolean assertive) implements ErrorMessageProvider {

    public MixsonEvent(MixsonCodec<T> codec, Predicate<ResourceLocation> resourcePredicate, String eventId, Event<T> event, ErrorPolciy errorPolciy, boolean assertive) {
        this(codec, UUID.randomUUID(), resourcePredicate, eventId, event, errorPolciy, assertive);
    }

    @Override
    public String getRuntimeErrorMessage(ResourceLocation resourceId) {
        return String.format("Failed to interact with %s file '%s' with event '%s'\n", codec.extensionAndDot(), resourceId, eventName);
    }

    @Override
    public String getRegistrationErrorMessage() {
        return String.format("Failed to register event %s\n", eventName);
    }

    @Override
    public ErrorPolciy getErrorPolicy() {
        return errorPolciy;
    }

    public Predicate<ResourceLocation> getWrappedPredicate() {
        return (id) -> {
            String ext = codec.extensionAndDot();
            if(!id.getPath().endsWith(ext))
                return false;
            ResourceLocation trimmedLocation = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath().substring(0, id.getPath().length() - ext.length()));
            return resourcePredicate.test(trimmedLocation);
        };
    }

    @Override
    public String getRegistrationMessage(int priority) {
        return String.format("Registering event '%s' with priority %s", eventName, priority);
    }
}
