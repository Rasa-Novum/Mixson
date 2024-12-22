package net.ramixin.mixson.events;

import com.google.gson.JsonElement;

@FunctionalInterface
public interface SimpleMixsonEvent extends MixsonEvent {

    JsonElement run(JsonElement elem);

}
