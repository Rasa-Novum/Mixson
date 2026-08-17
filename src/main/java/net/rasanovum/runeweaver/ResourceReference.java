package net.rasanovum.runeweaver;

import net.minecraft.resources.Identifier;
import net.rasanovum.runeweaver.enums.ErrorPolicy;
import net.rasanovum.runeweaver.util.Index;
import net.rasanovum.runeweaver.util.interfaces.ErrorMessageProvider;
import net.rasanovum.runeweaver.util.interfaces.RuneweaverCodec;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public class ResourceReference<T> implements ErrorMessageProvider {

    private T resource;
    private final String referenceName;
    private final Index index;
    private final UUID uuid = UUID.randomUUID();
    private final RuneweaverCodec<T> codec;
    private final int priority;

    protected ResourceReference(RuneweaverCodec<T> codec, int priority, Index index, String referenceName) {
        this.index = index;
        this.referenceName = referenceName;
        this.codec = codec;
        this.priority = priority;
    }

    public Optional<T> retrieve() {
        return Optional.ofNullable(resource);
    }

    public Optional<T> consume() {
        Optional<T> elem = retrieve();
        clear();
        return elem;
    }

    public void fulfill(T elem) {
        this.resource = elem;
    }

    public String getName() {
        return referenceName;
    }

    public Index getIndex() {
        return index;
    }

    public Identifier getResourceId() {
        return index.id();
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getOrdinal() {
        return index.ordinal();
    }

    public RuneweaverCodec<T> getCodec() {
        return codec;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public String getRuntimeErrorMessage(Identifier resourceId) {
        return String.format("Failed to capture %s file '%s' for reference '%s'\n", codec.extensionAndDot(), resourceId, referenceName);
    }

    @Override
    public String getRegistrationErrorMessage() {
        return String.format("Failed to register reference '%s' for file '%s'\n", referenceName, index);
    }

    @Override
    public ErrorPolicy getErrorPolicy() {
        return ErrorPolicy.THROW;
    }

    @Override
    public String getRegistrationMessage(int priority) {
        return String.format("Registering reference '%s' with priority %s", referenceName, priority);
    }

    protected void clear() {
        resource = null;
    }
}
