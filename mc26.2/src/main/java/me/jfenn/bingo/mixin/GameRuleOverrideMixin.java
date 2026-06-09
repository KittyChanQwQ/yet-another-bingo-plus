package me.jfenn.bingo.mixin;

import me.jfenn.bingo.mixinhandler.GameRuleOverrideHelper;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRules.class)
public class GameRuleOverrideMixin {
    @Inject(at = @At("HEAD"), method = "get", cancellable = true)
    <T> void getValueOverride(GameRule<T> rule, CallbackInfoReturnable<T> cir) {
        Object override = GameRuleOverrideHelper.INSTANCE.getGameRuleOverrides().get(rule.getIdentifier().getPath());
        if (override instanceof Boolean b) {
            cir.setReturnValue((T) b);
            cir.cancel();
        }
    }
}
