package me.jfenn.bingo.mixin;

import com.mojang.serialization.Lifecycle;
import me.jfenn.bingo.common.ConstantsKt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.server.integrated.IntegratedServerLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Hides the "Worlds using Experimental Settings" warning when creating a BINGO world
 */
@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {

    @Unique
    private final Logger log = LoggerFactory.getLogger(CreateWorldScreen.class);

    @Redirect(
            method = "createLevel()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/integrated/IntegratedServerLoader;tryLoad(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/gui/screen/world/CreateWorldScreen;Lcom/mojang/serialization/Lifecycle;Ljava/lang/Runnable;Z)V",
                    ordinal = 0
            )
    )
    private void tryLoad(MinecraftClient client, CreateWorldScreen parent, Lifecycle lifecycle, Runnable loader, boolean bypassWarnings) {
        if (parent.getWorldCreator().getWorldName().startsWith(ConstantsKt.getBINGO_WORLD_PREFIX())) {
            log.warn("Bypassing experimental warnings for a BINGO world");
            bypassWarnings = true;
        }

        IntegratedServerLoader.tryLoad(client, parent, lifecycle, loader, bypassWarnings);
    }

}
