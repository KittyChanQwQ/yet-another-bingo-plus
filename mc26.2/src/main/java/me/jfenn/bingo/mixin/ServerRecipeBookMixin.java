package me.jfenn.bingo.mixin;

import me.jfenn.bingo.mixinhelper.ServerRecipeBookMixinHelper;
import net.minecraft.stats.ServerRecipeBook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerRecipeBook.class)
public class ServerRecipeBookMixin {
    @ModifyArg(
            method = "lambda$addRecipes$0",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundRecipeBookAddPacket$Entry;<init>(Lnet/minecraft/world/item/crafting/display/RecipeDisplayEntry;ZZ)V"),
            index = 1
    )
    private static boolean showNotification(boolean showNotification) {
        if (ServerRecipeBookMixinHelper.getHideRecipeNotifications()) {
            return false;
        } else {
            return showNotification;
        }
    }
}
