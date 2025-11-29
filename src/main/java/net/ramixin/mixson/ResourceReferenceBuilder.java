package net.ramixin.mixson;

import net.ramixin.mixson.util.Index;
import net.ramixin.mixson.util.interfaces.MixsonCodec;

import java.util.Objects;

public class ResourceReferenceBuilder<T> {

    private MixsonCodec<T> codec;
    private Index index;
    private String referenceName;

    public ResourceReferenceBuilder<T> setCodec(MixsonCodec<T> codec) {
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

    public ResourceReference<T> build(int priority) {
        Objects.requireNonNull(codec, "codec must be set");
        Objects.requireNonNull(index, "index must be set");
        Objects.requireNonNull(referenceName, "reference name must be set");
        ResourceReference<T> ref = new ResourceReference<>(codec, index, referenceName);
        Mixson.logBasic(ref.getRegistrationMessage(priority));
        return ref;
    }

    public ResourceReferenceBuilder<T> copy() {
        return new ResourceReferenceBuilder<T>()
                .setCodec(codec)
                .setIndex(index.copy())
                .setReferenceName(String.valueOf(referenceName));
    }


}
