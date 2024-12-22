package net.ramixin.mixson;

import net.minecraft.util.Identifier;
import net.ramixin.mixson.events.MixsonEvent;

import java.util.UUID;

record AssociatedMixsonEvent(Identifier resourceId, Identifier eventId, MixsonEvent event, boolean silentlyFail, UUID... referenceIds) {
}
