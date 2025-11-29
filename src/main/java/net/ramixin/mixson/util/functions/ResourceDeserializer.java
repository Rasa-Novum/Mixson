package net.ramixin.mixson.util.functions;

import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;

@FunctionalInterface
public interface ResourceDeserializer<T> {

    T deserialize(Resource resource) throws IOException;

}
