package me.jfenn.bingo.mixinhandler

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.minecraft.server.network.ServerPlayerEntity

object PlayerEntityMixinHandler {

    /**
     * If the player is not on any team during a game, all their items
     * should be voided if dropped.
     */
    fun shouldVoidDroppedItems(player: ServerPlayerEntity): Boolean {
        val scope = BingoKoin.getScope(player.server) ?: return false
        val teamService = scope.get<TeamService>()
        val state = scope.get<BingoState>()
        val permissions = scope.get<PermissionsIntegration>()

        return state.state == GameState.PLAYING
                && teamService.getPlayerTeam(player) == null
                && !permissions.hasPermission(player, Permission.SPECTATOR_USE_ITEM)
    }

}