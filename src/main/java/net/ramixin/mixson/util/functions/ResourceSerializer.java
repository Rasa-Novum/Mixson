package net.ramixin.mixson.util.functions;

import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;

@FunctionalInterface
public interface ResourceSerializer<T> {

    Resource serialize(Resource associatedResource, T elem) throws IOException;

}
