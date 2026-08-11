package net.ramixin.mixson.rosetta;

/** Common initialization for the optional Mixson/Rosetta companion mod. */
public final class MixsonRosetta {
    public static final String MOD_ID = "mixson_rosetta";

    private MixsonRosetta() {}

    public static void initialize() {
        AssetChannels.initialize();
    }
}
