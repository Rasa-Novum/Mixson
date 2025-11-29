package net.ramixin.mixson.util;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Range;

public record Index(ResourceLocation id, @Range(from = -1, to = Integer.MAX_VALUE) int ordinal) implements Comparable<Index> {

    public Index(String stringId, int ordinal) {
        this(ResourceLocation.parse(stringId), ordinal);
    }

    public Index(ResourceLocation id) {
        this(id, -1);
    }

    public Index(String stringId) {
        this(ResourceLocation.parse(stringId), -1);
    }

    public Index withSuffixedId(String suffix) {
        return new Index(id.withSuffix(suffix), ordinal);
    }

    @Override
    public String toString() {
        return "Index{namespace=" +
                id.getNamespace() +
                ", path=" +
                id.getPath() +
                ", ordinal=" +
                ordinal +
                '}';
    }

    @Override
    public int compareTo(Index other) {
        int idCompare = this.id.compareTo(other.id);
        if (idCompare != 0)
            return idCompare;
        return Integer.compare(this.ordinal, other.ordinal);
    }


    public Index copy() {
        return new Index(ResourceLocation.parse(id.toString()), ordinal);
    }

}
