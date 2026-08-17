//? if forge {
package net.rasanovum.runeweaver.rosetta.loaders.forge;

import net.minecraftforge.fml.common.Mod;
import net.rasanovum.runeweaver.rosetta.RuneweaverRosetta;

@Mod(RuneweaverRosetta.MOD_ID)
public final class RuneweaverRosettaForge {
    public RuneweaverRosettaForge() {
        RuneweaverRosetta.initialize();
    }
}
//?}
