package me.jfenn.bingo.mixin;

import me.jfenn.bingo.mixinhandler.ServerPlayNetworkHandlerMixinHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(at = @At("HEAD"), method = "handleMovePlayer")
    private void onPlayerMove(CallbackInfo ci) {
        ServerPlayNetworkHandlerMixinHandler.INSTANCE.onPlayerMove(player);
    }
}
