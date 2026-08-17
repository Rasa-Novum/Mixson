package net.rasanovum.runeweaver.rosetta;

/** Common initialization for the optional Runeweaver/Rosetta companion mod. */
public final class RuneweaverRosetta {
    public static final String MOD_ID = "runeweaver_rosetta";

    private RuneweaverRosetta() {}

    public static void initialize() {
        AssetChannels.initialize();
    }
}
