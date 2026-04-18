package net.ramixin.mixson.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.hooks.StandardHook;
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
            //?} else {
            /*method = "loadContentsFromManager",
            *///?}
            at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/FileToIdConverter;listMatchingResources(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;")
    )
    private static Map<Identifier, Resource> runMixsonEvents(FileToIdConverter instance, ResourceManager resourceManager, Operation<Map<Identifier, Resource>> original) {
        return Mixson.processHook(new StandardHook(original.call(instance, resourceManager)));
    }
}
