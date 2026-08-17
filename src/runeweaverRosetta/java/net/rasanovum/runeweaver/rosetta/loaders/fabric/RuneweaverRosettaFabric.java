//? if fabric {
package net.rasanovum.runeweaver.rosetta.loaders.fabric;

import net.fabricmc.api.ModInitializer;
import net.rasanovum.runeweaver.rosetta.RuneweaverRosetta;

public final class RuneweaverRosettaFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        RuneweaverRosetta.initialize();
    }
}
//?}
