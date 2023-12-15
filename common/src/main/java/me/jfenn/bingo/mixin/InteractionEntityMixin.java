package me.jfenn.bingo.mixin;

import me.jfenn.bingo.event.InteractionEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.InteractionEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InteractionEntity.class)
abstract class InteractionEntityMixin extends Entity {

    public InteractionEntityMixin(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    @Inject(at = @At(value = "HEAD"), method = "handleAttack")
    public void handleAttack(Entity attacker, CallbackInfoReturnable<Boolean> ci) {
        var callback = InteractionEntityEvents.INSTANCE.getINTERACT_LISTENERS().get(uuid);
        if (callback != null && attacker instanceof ServerPlayerEntity player) {
            callback.invoke(player);
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "interact", cancellable = true)
    public void interact(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> ci) {
        var callback = InteractionEntityEvents.INSTANCE.getINTERACT_LISTENERS().get(uuid);
        if (callback != null && player instanceof ServerPlayerEntity serverPlayer) {
            callback.invoke(serverPlayer);
            ci.setReturnValue(ActionResult.SUCCESS);
            ci.cancel();
        }
    }

}
