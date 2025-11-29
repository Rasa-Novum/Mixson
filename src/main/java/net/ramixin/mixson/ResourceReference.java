package net.ramixin.mixson;

import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.enums.ErrorPolciy;
import net.ramixin.mixson.util.Index;
import net.ramixin.mixson.util.interfaces.ErrorMessageProvider;
import net.ramixin.mixson.util.interfaces.MixsonCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public class ResourceReference<T> implements ErrorMessageProvider {

    private T resource;

    private final String referenceName;

    private final Index index;

    private final UUID uuid = UUID.randomUUID();

    private final MixsonCodec<T> codec;

    protected ResourceReference(MixsonCodec<T> codec, @NotNull Index index, String referenceName) {
        this.index = index;
        this.referenceName = referenceName;
        this.codec = codec;
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

    public ResourceLocation getResourceId() {
        return index.id();
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getOrdinal() {
        return index.ordinal();
    }

    public MixsonCodec<T> getCodec() {
        return codec;
    }

    @Override
    public String getRuntimeErrorMessage(ResourceLocation resourceId) {
        return String.format("Failed to capture %s file '%s' for reference '%s'\n", codec.extensionAndDot(), resourceId, referenceName);
    }

    @Override
    public String getRegistrationErrorMessage() {
        return String.format("Failed to register reference '%s' for file '%s'\n", referenceName, index);
    }

    @Override
    public ErrorPolciy getErrorPolicy() {
        return ErrorPolciy.THROW;
    }

    @Override
    public String getRegistrationMessage(int priority) {
        return String.format("Registering reference '%s' with priority %s", referenceName, priority);
    }

    protected void clear() {
        resource = null;
    }
}
