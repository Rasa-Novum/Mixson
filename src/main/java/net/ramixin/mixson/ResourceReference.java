package net.ramixin.mixson;

import net.minecraft.util.Identifier;

public record ResourceReference(int priority, Identifier resourceId, Identifier referenceId) {
}
