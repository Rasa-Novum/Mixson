//? if forge {
package net.ramixin.mixson.rosetta.loaders.forge;

import net.minecraftforge.fml.common.Mod;
import net.ramixin.mixson.rosetta.MixsonRosetta;

@Mod(MixsonRosetta.MOD_ID)
public final class MixsonRosettaForge {
    public MixsonRosettaForge() {
        MixsonRosetta.initialize();
    }
}
//?}
