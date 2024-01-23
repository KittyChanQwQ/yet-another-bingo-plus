package me.jfenn.bingo.integrations

import me.jfenn.bingo.common.MOD_ID
import net.minecraft.server.network.ServerPlayerEntity
import me.lucko.fabric.api.permissions.v0.Permissions

interface PermissionsIntegration {

    fun canUseGameMenu(player: ServerPlayerEntity): Boolean

    fun canUsePlayerMenu(player: ServerPlayerEntity): Boolean

    /** "/join {team}" command */
    fun canUseJoinCommand(player: ServerPlayerEntity): Boolean

    /** "/join {team} {player}" command */
    fun canUseJoinPlayerCommand(player: ServerPlayerEntity): Boolean

    /** "/coords" command */
    fun canUseCoordsCommand(player: ServerPlayerEntity): Boolean

    /** "/restart" command */
    fun canUseRestartCommand(player: ServerPlayerEntity): Boolean

    /** "/bingo ..." commands */
    fun canUseBingoCommand(player: ServerPlayerEntity): Boolean

    /** "/bingo set" command */
    fun canUseBingoSetCommand(player: ServerPlayerEntity): Boolean

    fun canBypassLobbyChaosPrevention(player: ServerPlayerEntity): Boolean

    companion object {
        fun create(): PermissionsIntegration {
            return PermissionsIntegrationImpl()
        }
    }

}

class PermissionsIntegrationImpl : PermissionsIntegration {

    private fun hasDefaultPermission(player: ServerPlayerEntity) = player.hasPermissionLevel(2) || player.server.isSingleplayer

    override fun canUseGameMenu(player: ServerPlayerEntity)
        = Permissions.check(player, "$MOD_ID.lobbymenu", hasDefaultPermission(player))

    override fun canUsePlayerMenu(player: ServerPlayerEntity)
        = Permissions.check(player, "$MOD_ID.playermenu", true)

    override fun canUseJoinCommand(player: ServerPlayerEntity)
        = Permissions.check(player, "$MOD_ID.command.join", true)

    override fun canUseJoinPlayerCommand(player: ServerPlayerEntity)
        = Permissions.check(player, "$MOD_ID.command.join_player", hasDefaultPermission(player))

    override fun canUseCoordsCommand(player: ServerPlayerEntity)
        = Permissions.check(player, "$MOD_ID.command.coords", true)

    override fun canUseRestartCommand(player: ServerPlayerEntity)
        = Permissions.check(player, "$MOD_ID.command.restart", 2)

    override fun canUseBingoCommand(player: ServerPlayerEntity)
        = Permissions.check(player, "$MOD_ID.command.bingo", hasDefaultPermission(player))

    override fun canUseBingoSetCommand(player: ServerPlayerEntity)
        = Permissions.check(player, "$MOD_ID.command.bingo_set", true)

    override fun canBypassLobbyChaosPrevention(player: ServerPlayerEntity)
        = Permissions.check(player, "$MOD_ID.lobby.chaos_prevention.bypass", 2)

}
