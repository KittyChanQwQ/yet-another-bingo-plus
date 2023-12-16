package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.config.PlayerSettings
import me.jfenn.bingo.common.utils.formatTitle
import net.minecraft.entity.EntityType
import net.minecraft.nbt.NbtFloat
import net.minecraft.nbt.NbtList
import net.minecraft.particle.DustParticleEffect
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import org.joml.Vector3f

fun MenuComponent.registerPlayerMenu(
    pos: Vec3d,
    facing: Direction,
) {
    registerEntity(EntityType.TEXT_DISPLAY, pos) {
        put("Rotation", NbtList().apply {
            add(NbtFloat.of(facing.asRotation()))
            add(NbtFloat.of(0f))
        })
        putString("text", "\"Player Settings\"")
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }

    val fields = listOf(
        PlayerSettings::scoreboard,
        PlayerSettings::bossbar,
        PlayerSettings::leadingMessages,
        PlayerSettings::scoreMessages,
    )

    fields.forEachIndexed { i, field ->
        val fieldPosition = pos.add(0.0, -(MENU_ITEM_OFFSET + i * MENU_ITEM_HEIGHT), 0.0)

        registerButton(
            position = fieldPosition,
            text = field.name.formatTitle(),
            facing = facing,
            requiredPermissionLevel = 0,
        ) {
            val player = it as? ServerPlayerEntity ?: return@registerButton
            val config = ConfigService.getPlayer(player.uuidAsString)
            val value = field.get(config)
            ConfigService.setPlayerConfig(player, field, !value)
        }

        onTick {
            if (it.ctx.server.ticks % 4 != 0) return@onTick
            for (player in it.ctx.server.playerManager.playerList) {
                if (player.z > fieldPosition.z || player.pos.distanceTo(fieldPosition) > 8.0) continue

                val config = ConfigService.getPlayer(player.uuidAsString)
                val value = field.get(config)
                val color = if (value) Vector3f(0f, 1f, 0f) else Vector3f(1f, 0f, 0f)
                val effect = DustParticleEffect(color, 1f)
                it.ctx.lobbyWorld.spawnParticles(player, effect, false, fieldPosition.x + 1.2, fieldPosition.y + 0.2, fieldPosition.z, 1, 0.0, 0.0, 0.0, 1.0)
            }
        }
    }
}