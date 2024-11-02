package me.jfenn.bingo.integrations

import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.PermissionDefault
import me.jfenn.bingo.common.PermissionKey
import me.lucko.fabric.api.permissions.v0.Permissions
import org.slf4j.Logger
import java.util.*

interface PermissionsIntegration {

    fun hasPermission(player: IPlayerHandle, key: PermissionKey): Boolean

    companion object {
        fun create(log: Logger): PermissionsIntegration {
            return PermissionsIntegrationImpl(log)
        }
    }

}

class PermissionsIntegrationImpl(
    private val log: Logger,
) : PermissionsIntegration {

    init {
        val uuid = UUID.randomUUID()
        for (key in Permission.getPermissions()) {
            Permissions.check(uuid, key.permission)
        }
    }

    override fun hasPermission(player: IPlayerHandle, key: PermissionKey): Boolean {
        val fallback = when (key.default) {
            PermissionDefault.ALLOW -> true
            PermissionDefault.OPERATORS -> player.player.server.isSingleplayer || player.hasPermissionLevel(2)
            PermissionDefault.DENY -> false
        }
        return try {
            Permissions.check(player.commandSource, key.permission, fallback)
        } catch (e: Throwable) {
            log.error("[PermissionsIntegrationImpl] Permission check failed (${e::class.simpleName}), using fallback")
            fallback
        }
    }

}
