package me.jfenn.bingo.mixin;

import me.jfenn.bingo.mixinhandler.PlayerEntityMixinHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private void addPickBlock(ServerPlayer player, ItemStack stack) {
        Inventory inv = player.getInventory();
        int i = inv.getSlotWithRemainingSpace(stack);
        if (Inventory.isHotbarSlot(i)) {
            inv.setSelectedSlot(i);
        } else {
            if (i == -1) {
                inv.setSelectedSlot(inv.getSuitableHotbarSlot());
                if (inv.getItem(inv.getSelectedSlot()).isEmpty()) {
                    inv.setItem(inv.getSelectedSlot(), stack);
                } else {
                    int j = inv.getFreeSlot();
                    if (j != -1) {
                        inv.setItem(j, stack);
                    }
                }
            } else {
                inv.pickSlot(i);
            }
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "createItemStackToDrop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", cancellable = true)
    public void dropPlayerItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> ci) {
        if ((Object) this instanceof ServerPlayer player) {
            if (PlayerEntityMixinHandler.Companion.shouldVanishDrop(stack)) {
                if (PlayerEntityMixinHandler.Companion.shouldKeepDrop(stack)) {
                    ItemStack cur = player.getMainHandItem();
                    if (retainOwnership && (cur == null || cur.isEmpty())) {
                        // The complete stack was dropped
                        addPickBlock(player, stack.copy());
                    } else {
                        // Fallback
                        player.getInventory().add(stack.copy());
                    }
                }

                stack.setCount(0);
                ci.setReturnValue(null);
                ci.cancel();
            }
        }
    }

}
