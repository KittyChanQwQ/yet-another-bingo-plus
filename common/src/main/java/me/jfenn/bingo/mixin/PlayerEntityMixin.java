package me.jfenn.bingo.mixin;

import me.jfenn.bingo.common.Bingo;
import me.jfenn.bingo.common.ConstantsKt;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Shadow
    public abstract PlayerInventory getInventory();

    @Inject(at = @At(value = "HEAD"), method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", cancellable = true)
    public void dropItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> ci) {
        PlayerEntity playerEntity = (PlayerEntity) (Object) this;
        var ctx = Bingo.INSTANCE.getContext(playerEntity.getServer());
        if (ctx == null) return;

        // if the item has the bingo_vanish flag, it should not drop as an entity
        if (stack.hasNbt() && stack.getNbt().getBoolean(ConstantsKt.NBT_BINGO_VANISH)) {
            ItemStack cur = getInventory().getMainHandStack();
            if (retainOwnership && (cur == null || cur.isEmpty())) {
                // The complete stack was dropped
                getInventory().addPickBlock(stack.copy());
            } else {
                // Fallback
                getInventory().insertStack(stack.copy());
            }

            ci.setReturnValue(null);
            ci.cancel();
        }
    }

}
