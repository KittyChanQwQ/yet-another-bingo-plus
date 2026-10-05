package me.jfenn.bingo.mixin;

import me.jfenn.bingo.impl.item.ItemStackImpl;
import me.jfenn.bingo.mixinhelper.ItemStackMixinAccess;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ItemStackMixinAccess {
    @Unique
    ItemStackImpl yamb$impl;

    @Override
    public ItemStackImpl yamb$getItemStackImpl() {
         if (yamb$impl == null) {
             yamb$impl = new ItemStackImpl((ItemStack) (Object) this);
         }
         return yamb$impl;
    }
}
