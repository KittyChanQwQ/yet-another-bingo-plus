package me.jfenn.bingo.client.mixin;

import me.jfenn.bingo.api.IObjectiveHandle;
import me.jfenn.bingo.common.card.BingoTeam;
import me.jfenn.bingo.impl.ObjectiveHandle;
import me.jfenn.bingo.mixinhandler.ScoreboardMixinHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Scoreboard.class)
public abstract class ScoreboardMixin {
    @Inject(
            method = "getObjectiveForSlot",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void getObjectiveForSlot(ScoreboardDisplaySlot slot, CallbackInfoReturnable<ScoreboardObjective> ci) {
        if (slot != ScoreboardDisplaySlot.SIDEBAR) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        IntegratedServer server = client.getServer();
        if (server == null) return;

        BingoTeam team = BingoTeam.Companion.fromPlayer(server, player);
        IObjectiveHandle handle = ScoreboardMixinHandler.INSTANCE.getScoreboardObjective(server, team);
        if (handle instanceof ObjectiveHandle) {
            ScoreboardObjective objective = ((ObjectiveHandle) handle).getObjective();

            boolean shouldDisplay = ScoreboardMixinHandler.INSTANCE.shouldDisplayScoreboard(server, player);
            if (shouldDisplay) {
                ci.setReturnValue(objective);
                ci.cancel();
            }
        }
    }
}
