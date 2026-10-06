package me.jfenn.bingo.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Lifecycle;
import me.jfenn.bingo.client.world.WorldMixinHelper;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldOpenFlows.class)
public class WorldOpenFlowsMixin {
    @WrapOperation(
            method = "openWorldCheckWorldStemCompatibility",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/WorldData;worldGenSettingsLifecycle()Lcom/mojang/serialization/Lifecycle;")
    )
    Lifecycle worldGenSettingsLifecycle(WorldData instance, Operation<Lifecycle> original) {
        if (WorldMixinHelper.isBingoWorld(instance)) {
            // Skip the experimental warning when opening a bingo world
            return Lifecycle.stable();
        } else {
            return original.call(instance);
        }
    }
}
