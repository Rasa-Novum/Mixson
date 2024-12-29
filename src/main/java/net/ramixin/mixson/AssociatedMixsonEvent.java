package net.ramixin.mixson;

import net.minecraft.util.Identifier;
import net.ramixin.mixson.events.MixsonEventTypes;

import java.util.UUID;

record AssociatedMixsonEvent(Identifier resourceId, Identifier eventId, MixsonEventTypes.BaseEvent event, boolean silentlyFail, boolean referenceEvent, UUID... referenceIds) {
}
