//? if fabric {
package net.ramixin.mixson.rosetta.loaders.fabric;

import net.fabricmc.api.ModInitializer;
import net.ramixin.mixson.rosetta.MixsonRosetta;

public final class MixsonRosettaFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MixsonRosetta.initialize();
    }
}
//?}
