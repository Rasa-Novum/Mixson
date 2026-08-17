package net.rasanovum.runeweaver.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.rasanovum.runeweaver.Runeweaver;
import net.rasanovum.runeweaver.hooks.StandardHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

//? if >=26.1 {
@Mixin(targets = "net.minecraft.resources.ResourceManagerRegistryLoadTask")
//?} else {
/*@Mixin(targets = "net.minecraft.resources.RegistryDataLoader")
*///?}
public class ResourceManagerRegistryLoadTaskMixin {

    @WrapOperation(
            //? if >=26.1 {
            method = "lambda$load$0",
            //?} else if >1.20.1 {
            /*method = "loadContentsFromManager",
            *///?} else {
            /*method = "loadRegistryContents",
            *///?}
            at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/FileToIdConverter;listMatchingResources(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;")
    )
    private static Map<Identifier, Resource> runRuneweaverEvents(FileToIdConverter instance, ResourceManager resourceManager, Operation<Map<Identifier, Resource>> original) {
        return Runeweaver.processHook(new StandardHook(original.call(instance, resourceManager)));
    }
}
