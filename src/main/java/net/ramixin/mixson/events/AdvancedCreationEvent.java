package net.ramixin.mixson.events;

import com.google.gson.JsonElement;
import net.minecraft.util.Identifier;
import net.ramixin.mixson.BuiltResourceReference;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

@FunctionalInterface
public interface AdvancedCreationEvent extends MixsonEventTypes.Creation {

    @Nullable JsonElement run(HashMap<Identifier, BuiltResourceReference> references);

}
