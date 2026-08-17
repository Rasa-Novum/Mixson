package net.rasanovum.runeweaver;

import net.minecraft.resources.Identifier;
import net.rasanovum.runeweaver.enums.ErrorPolicy;
import net.rasanovum.runeweaver.enums.Lifetime;
import net.rasanovum.runeweaver.util.Index;
import net.rasanovum.runeweaver.util.VersionUtils;
import net.rasanovum.runeweaver.util.functions.Event;
import net.rasanovum.runeweaver.util.interfaces.ErrorMessageProvider;
import net.rasanovum.runeweaver.util.interfaces.RuneweaverCodec;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;
import java.util.function.Predicate;

@ApiStatus.Internal
public record RuneweaverEvent<T>(UUID uuid, RuneweaverCodec<T> codec, int priority, Lifetime lifetime, ErrorPolicy errorPolicy, String eventName, Predicate<Index> resourcePredicate, Event<T> event, boolean assertive) implements ErrorMessageProvider {

    public RuneweaverEvent(RuneweaverCodec<T> codec, int priority, Lifetime lifetime, ErrorPolicy errorPolicy, String eventName, Predicate<Index> resourcePredicate, Event<T> event, boolean assertive) {
        this(UUID.randomUUID(), codec, priority, lifetime, errorPolicy, eventName, resourcePredicate, event, assertive);
    }

    @Override
    public String getRuntimeErrorMessage(Identifier resourceId) {
        return String.format("Failed to interact with %s file '%s' with event '%s'\n", codec.extensionAndDot(), resourceId, eventName);
    }

    @Override
    public String getRegistrationErrorMessage() {
        return String.format("Failed to register event %s\n", eventName);
    }

    @Override
    public ErrorPolicy getErrorPolicy() {
        return errorPolicy;
    }

    public Predicate<Index> getWrappedPredicate() {
        return (index) -> {
            String ext = codec.extensionAndDot();
            Identifier id = index.id();
            if(!VersionUtils.path(id).endsWith(ext))
                return false;
            Identifier trimmedLocation = VersionUtils.id(VersionUtils.namespace(id), VersionUtils.path(id).substring(0, VersionUtils.path(id).length() - ext.length()));
            return resourcePredicate.test(new Index(trimmedLocation, index.ordinal()));
        };
    }

    @Override
    public String getRegistrationMessage(int priority) {
        return String.format("Registering event '%s' with priority %s", eventName, priority);
    }
}
