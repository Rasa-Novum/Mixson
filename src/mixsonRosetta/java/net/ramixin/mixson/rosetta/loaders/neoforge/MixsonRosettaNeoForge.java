//? if neoforge {
package net.ramixin.mixson.rosetta.loaders.neoforge;

import net.neoforged.fml.common.Mod;
import net.ramixin.mixson.rosetta.MixsonRosetta;

@Mod(MixsonRosetta.MOD_ID)
public final class MixsonRosettaNeoForge {
    public MixsonRosettaNeoForge() {
        MixsonRosetta.initialize();
    }
}
//?}
