package me.jfenn.bingo.integrations

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.PermissionDefault
import me.jfenn.bingo.common.PermissionKey
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.server.network.ServerPlayerEntity
import java.util.UUID

interface PermissionsIntegration {

    fun hasPermission(player: ServerPlayerEntity, key: PermissionKey): Boolean

    companion object {
        fun create(): PermissionsIntegration {
            return PermissionsIntegrationImpl()
        }
    }

}

class PermissionsIntegrationImpl : PermissionsIntegration {

    init {
        val uuid = UUID.randomUUID()
        for (key in Permission.getPermissions()) {
            Permissions.check(uuid, key.permission)
        }
    }

    override fun hasPermission(player: ServerPlayerEntity, key: PermissionKey): Boolean {
        return Permissions.check(player, key.permission, when (key.default) {
            PermissionDefault.ALLOW -> true
            PermissionDefault.OPERATORS -> player.server.isSingleplayer || player.hasPermissionLevel(2)
            PermissionDefault.DENY -> false
        })
    }

}
