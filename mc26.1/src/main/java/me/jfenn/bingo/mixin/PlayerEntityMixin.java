package me.jfenn.bingo.mixin;

import me.jfenn.bingo.impl.TextImpl;
import me.jfenn.bingo.mixinhandler.PlayerEntityMixinHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class PlayerEntityMixin {

    @Unique
    private void addPickBlock(ItemStack stack) {
        ServerPlayer player = (ServerPlayer) (Object) this;
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

    @Inject(at = @At(value = "HEAD"), method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", cancellable = true)
    public void dropPlayerItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> ci) {
        if ((Object) this instanceof ServerPlayer player) {
            if (PlayerEntityMixinHandler.Companion.shouldVanishDrop(stack)) {
                if (PlayerEntityMixinHandler.Companion.shouldKeepDrop(stack)) {
                    ItemStack cur = player.getMainHandItem();
                    if (retainOwnership && (cur == null || cur.isEmpty())) {
                        // The complete stack was dropped
                        addPickBlock(stack.copy());
                    } else {
                        // Fallback
                        player.getInventory().add(stack.copy());
                    }
                }

                ci.setReturnValue(null);
                ci.cancel();
            }
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "getTabListDisplayName", cancellable = true)
    public void getPlayerListName(CallbackInfoReturnable<Component> ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        var originalName = player.getDisplayName();
        var name = PlayerEntityMixinHandler.Companion.getPlayerListName(
                player.getUUID(),
                new TextImpl(originalName != null ? originalName.copy() : Component.literal(player.getScoreboardName()))
        );
        if (name != null) {
            ci.setReturnValue(name.getValue());
            ci.cancel();
        }
    }

}
