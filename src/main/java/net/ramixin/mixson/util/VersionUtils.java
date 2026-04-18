package net.ramixin.mixson.util;

import net.minecraft.resources.Identifier;

import java.util.Objects;

public final class VersionUtils {

    private VersionUtils() {
    }

    public static Identifier id(String value) {
        //? if >1.20.1 {
        return Identifier.parse(value);
        //?} else {
        /*return new Identifier(value);
        *///?}
    }

    public static Identifier id(String namespace, String path) {
        //? if >1.20.1 {
        return Identifier.fromNamespaceAndPath(namespace, path);
        //?} else {
        /*return new Identifier(namespace, path);
        *///?}
    }

    public static Identifier withSuffix(Identifier id, String suffix) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(suffix, "suffix");
        //? if >=26.1 {
        return id.withSuffix(suffix);
        //?} else {
        /*return id.withPath(path(id) + suffix);
        *///?}
    }

    public static String namespace(Identifier id) {
        return id.getNamespace();
    }

    public static String path(Identifier id) {
        return id.getPath();
    }
}
