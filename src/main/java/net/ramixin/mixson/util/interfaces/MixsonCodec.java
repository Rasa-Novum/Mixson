package net.ramixin.mixson.util.interfaces;

import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.util.functions.ResourceDeserializer;
import net.ramixin.mixson.util.functions.ResourceExporter;
import net.ramixin.mixson.util.functions.ResourceSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface MixsonCodec<T> extends ResourceDeserializer<T>, ResourceSerializer<T>, ResourceExporter<T> {

    String extensionAndDot();

    static <T> MixsonCodec<T> create(String extension, ResourceDeserializer<T> deserializer, ResourceSerializer<T> serializer, ResourceExporter<T> outputFileSerializer) {
        return new MixsonCodec<>() {
            @Override
            public T deserialize(Resource resource) throws IOException {
                return deserializer.deserialize(resource);
            }

            @Override
            public Resource serialize(Resource associatedResource, T file) throws IOException {
                return serializer.serialize(associatedResource, file);
            }

            @Override
            public ByteArrayOutputStream export(T file) throws IOException {
                return outputFileSerializer.export(file);
            }

            @Override
            public String extensionAndDot() {
                return '.'+extension;
            }
        };
    }
}
