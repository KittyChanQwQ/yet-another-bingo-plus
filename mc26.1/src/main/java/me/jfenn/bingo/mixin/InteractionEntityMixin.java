package me.jfenn.bingo.mixin;

import me.jfenn.bingo.common.event.InteractionEntityEvents;
import me.jfenn.bingo.impl.InteractionEntityImpl;
import me.jfenn.bingo.impl.PlayerHandle;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// TODO(Ravel): can not resolve target class InteractionEntity
@Mixin(Interaction.class)
abstract class InteractionEntityMixin extends Entity {

    public InteractionEntityMixin(EntityType<?> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(at = @At(value = "HEAD"), method = "skipAttackInteraction")
    public void handleAttack(Entity attacker, CallbackInfoReturnable<Boolean> ci) {
        if (attacker instanceof ServerPlayer serverPlayer) {
            MinecraftServer server = serverPlayer.level().getServer();
            if (server != null) {
                InteractionEntityEvents.triggerInteract(
                        new InteractionEntityImpl((Interaction) (Object) this),
                        new PlayerHandle(serverPlayer, null),
                        server
                );
            }
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "interact", cancellable = true)
    public void interact(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            MinecraftServer server = serverPlayer.level().getServer();
            if (server != null) {
                boolean success = InteractionEntityEvents.triggerInteract(
                        new InteractionEntityImpl((Interaction) (Object) this),
                        new PlayerHandle(serverPlayer, null),
                        server
                );

                if (success) {
                    // For some reason the hand swing is broken in 1.21.2
                    serverPlayer.swing(InteractionHand.MAIN_HAND, true);
                    ci.setReturnValue(InteractionResult.SUCCESS);
                    ci.cancel();
                }
            }
        }
    }
}
