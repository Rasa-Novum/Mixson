package net.ramixin.mixson.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.hooks.ListHook;
import net.ramixin.mixson.hooks.StandardHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Map;

@Mixin({MultiPackResourceManager.class, ReloadableResourceManager.class})
public class ResourceManagerImplMixinArray {

    @ModifyReturnValue(method = "listResources", at = @At("RETURN"))
    private Map<ResourceLocation, Resource> runMixsonEvents(Map<ResourceLocation, Resource> original) {
        return Mixson.processHook(new StandardHook(original));
    }

    @ModifyReturnValue(method = "listResourceStacks", at = @At("RETURN"))
    private Map<ResourceLocation, List<Resource>> runListMixsonEvents(Map<ResourceLocation, List<Resource>> original) {
        return Mixson.processHook(new ListHook(original));
    }

}
