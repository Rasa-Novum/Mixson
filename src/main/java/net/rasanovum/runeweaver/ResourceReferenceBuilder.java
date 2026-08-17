package net.rasanovum.runeweaver;

import net.rasanovum.runeweaver.util.Index;
import net.rasanovum.runeweaver.util.interfaces.RuneweaverCodec;

import java.util.Objects;

public class ResourceReferenceBuilder<T> {

    private RuneweaverCodec<T> codec;
    private Index index;
    private String referenceName;
    private int priority = Runeweaver.DEFAULT_PRIORITY;

    public ResourceReferenceBuilder<T> setCodec(RuneweaverCodec<T> codec) {
        this.codec = codec;
        return this;
    }

    public ResourceReferenceBuilder<T> setIndex(Index index) {
        this.index = index;
        return this;
    }

    public ResourceReferenceBuilder<T> setReferenceName(String referenceNew) {
        this.referenceName = referenceNew;
        return this;
    }

    public ResourceReferenceBuilder<T> setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public ResourceReference<T> build() {
        Objects.requireNonNull(codec, "codec must be set");
        Objects.requireNonNull(index, "index must be set");
        Objects.requireNonNull(referenceName, "reference name must be set");
        ResourceReference<T> ref = new ResourceReference<>(codec, priority, index, referenceName);
        Runeweaver.logBasic(ref.getRegistrationMessage(priority));
        return ref;
    }

    public ResourceReferenceBuilder<T> copy() {
        return new ResourceReferenceBuilder<T>()
                .setCodec(codec)
                .setIndex(index.copy())
                .setPriority(priority)
                .setReferenceName(String.valueOf(referenceName));
    }
}
