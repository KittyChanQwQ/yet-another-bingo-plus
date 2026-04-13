package me.jfenn.bingo.common

import me.jfenn.bingo.integrations.permissions.PermissionDefault
import me.jfenn.bingo.integrations.permissions.PermissionKey
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

internal object Permission {

    val CONFIGURE_LOBBY = PermissionKey("$BINGO.configure.lobby", PermissionDefault.OPERATORS)

    /** "/bingo ..." commands (& lobby game menu) */
    val CONFIGURE_GAME = PermissionKey("$BINGO.configure.game", PermissionDefault.OPERATORS)

    /** "/bingo set" command (& lobby player menu) */
    val CONFIGURE_PLAYER = PermissionKey("$BINGO.configure.player", PermissionDefault.ALLOW)

    /** "/join {team}" command */
    val COMMAND_JOIN = PermissionKey("$BINGO.command.join", PermissionDefault.ALLOW)

    /** "/join {team} {player}" command */
    val COMMAND_JOIN_PLAYER = PermissionKey("$BINGO.command.join_player", PermissionDefault.OPERATORS)

    /** "/ready" command */
    val COMMAND_READY = PermissionKey("$BINGO.command.ready", PermissionDefault.ALLOW)

    /** "/coords" command */
    val COMMAND_COORDS = PermissionKey("$BINGO.command.coords", PermissionDefault.ALLOW)

    /** "/spectator" command */
    val COMMAND_SPECTATOR = PermissionKey("$BINGO.command.spectator", PermissionDefault.ALLOW)

    /** "/bingo reset" command */
    val COMMAND_RESET = PermissionKey("$BINGO.command.reset", PermissionDefault.OPERATORS)

    /** "/bingodata" commands */
    val COMMAND_DATA = PermissionKey("$BINGO.command.bingodata", PermissionDefault.OPERATORS)

    /** "/bingo debug" command */
    val COMMAND_DEBUG = PermissionKey("$BINGO.command.debug", PermissionDefault.OPERATORS)

    /** Make sounds in the lobby, even if lobbyChaosPrevention=true */
    val BYPASS_CHAOS_PREVENTION = PermissionKey("$BINGO.lobby.bypass_chaos_prevention", PermissionDefault.OPERATORS)

    /** Interact with blocks/doors in vanished spectator mode */
    val SPECTATOR_USE_DOORS = PermissionKey("$BINGO.spectator.use_doors", PermissionDefault.DENY)

    /** View inventories in spectator */
    val SPECTATOR_VIEW_INVENTORY = PermissionKey("$BINGO.spectator.view_inventory", PermissionDefault.ALLOW)

    /** View all team cards in spectator */
    val SPECTATOR_VIEW_CARDS = PermissionKey("$BINGO.spectator.view_cards", PermissionDefault.ALLOW)

    /** View a player's own statistics */
    val STATS_VIEW_SELF = PermissionKey("$BINGO.stats.view_self", PermissionDefault.ALLOW)

    /** View another player's statistics */
    val STATS_VIEW_PLAYER = PermissionKey("$BINGO.stats.view_player", PermissionDefault.OPERATORS)

    /** Clear a player's own statistics */
    val STATS_RESET_SELF = PermissionKey("$BINGO.stats.reset_self", PermissionDefault.ALLOW)

    /** Clear server-wide statistics */
    val STATS_RESET_GLOBAL = PermissionKey("$BINGO.stats.reset_global", PermissionDefault.OPERATORS)

    fun getPermissions() = this::class.declaredMemberProperties
        .filter { it.returnType == PermissionKey::class }
        .map { @Suppress("UNCHECKED_CAST") (it as KProperty1<Permission, PermissionKey>).get(this) }

}