package me.jfenn.bingo.common

class PermissionKey(
    val permission: String,
    val default: PermissionDefault,
)

enum class PermissionDefault {
    ALLOW,
    OPERATORS,
    DENY,
}

object Permission {

    /** "/bingo ..." commands (& lobby game menu) */
    val CONFIGURE_GAME = PermissionKey("$MOD_ID.configure.game", PermissionDefault.OPERATORS)

    /** "/bingo set" command (& lobby player menu) */
    val CONFIGURE_PLAYER = PermissionKey("$MOD_ID.configure.player", PermissionDefault.ALLOW)

    /** "/join {team}" command */
    val COMMAND_JOIN = PermissionKey("$MOD_ID.command.join", PermissionDefault.ALLOW)

    /** "/join {team} {player}" command */
    val COMMAND_JOIN_PLAYER = PermissionKey("$MOD_ID.command.join_player", PermissionDefault.OPERATORS)

    /** "/coords" command */
    val COMMAND_COORDS = PermissionKey("$MOD_ID.command.coords", PermissionDefault.ALLOW)

    /** "/spectator" command */
    val COMMAND_SPECTATOR = PermissionKey("$MOD_ID.command.spectator", PermissionDefault.ALLOW)

    /** "/restart" command */
    val COMMAND_RESTART = PermissionKey("$MOD_ID.command.restart", PermissionDefault.OPERATORS)

    /** Make sounds in the lobby, even if lobbyChaosPrevention=true */
    val BYPASS_CHAOS_PREVENTION = PermissionKey("$MOD_ID.lobby.bypass_chaos_prevention", PermissionDefault.OPERATORS)

    /** Interact with blocks/doors in vanished spectator mode */
    val SPECTATOR_USE_DOORS = PermissionKey("$MOD_ID.spectator.use_doors", PermissionDefault.DENY)

    /** View inventories in spectator */
    val SPECTATOR_VIEW_INVENTORY = PermissionKey("$MOD_ID.spectator.view_inventory", PermissionDefault.ALLOW)

    /** View all team cards in spectator */
    val SPECTATOR_VIEW_CARDS = PermissionKey("$MOD_ID.spectator.view_cards", PermissionDefault.ALLOW)

}