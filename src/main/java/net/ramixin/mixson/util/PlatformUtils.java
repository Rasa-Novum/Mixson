package net.ramixin.mixson.util;

//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
//?} else if neoforge {
/*import net.neoforged.fml.loading.FMLPaths;
*///?} else {
/*import net.minecraftforge.fml.loading.FMLPaths;
*///?}

import java.nio.file.Path;

public final class PlatformUtils {

    private PlatformUtils() {
    }

    public static Path getGameDir() {
        //? if fabric {
        return FabricLoader.getInstance().getGameDir();
        //?} else {
        /*return FMLPaths.GAMEDIR.get();
        *///?}
    }
}
