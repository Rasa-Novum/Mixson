package net.ramixin.mixson.events;

import net.minecraft.util.Identifier;
import net.ramixin.mixson.BuiltResourceReference;

import java.util.HashMap;

@FunctionalInterface
public interface AdvancedDeletionEvent extends MixsonEventTypes.Deletion {
    
    boolean run(HashMap<Identifier, BuiltResourceReference> references);

    @Override
    default String getName() {
        return "Advanced Deletion Event";
    }
}
