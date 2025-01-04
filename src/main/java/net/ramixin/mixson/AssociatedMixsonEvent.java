package net.ramixin.mixson;

import net.minecraft.util.Identifier;
import net.ramixin.mixson.events.MixsonEventTypes;

import java.util.UUID;

public record AssociatedMixsonEvent(Identifier resourceId, Identifier eventId, MixsonEventTypes.BaseEvent<?> event, boolean silentlyFail, boolean referenceEvent, UUID... referenceIds) {
}
