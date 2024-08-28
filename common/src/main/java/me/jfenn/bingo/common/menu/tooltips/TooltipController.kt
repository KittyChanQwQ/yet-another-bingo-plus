package me.jfenn.bingo.common.menu.tooltips

import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.common.LOBBY_WORLD_ID
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*
import kotlin.math.pow

class TooltipController(
    private val tooltipService: TooltipService,
    private val packets: ServerPacketEvents,
    private val playerManager: IPlayerManager,
    events: ScopedEvents
) {

    private val lookingAtTicks = mutableMapOf<Pair<UUID, UUID>, Int>()

    fun tick() {
        val prevTicks = lookingAtTicks.toMap()
        lookingAtTicks.clear()

        for (player in playerManager.getPlayers()) {
            if (player.serverWorld.registryKey.value == LOBBY_WORLD_ID) {
                sendTooltip(player, prevTicks)
            }
        }
    }

    fun sendTooltip(player: IPlayerHandle, prevTicks: Map<Pair<UUID, UUID>, Int>) {
        val reachDistance = 1.5
        val origin = with(player.player.eyePos) { Vec3d(x, y, z) }

        val pitch = Quaternionf().fromAxisAngleDeg(Vector3f(1f, 0f, 0f), -player.pitch)
        val yaw = Quaternionf().fromAxisAngleDeg(Vector3f(0f, 1f, 0f), -player.yaw)
        val direction = pitch.mul(yaw)
            .transform(Vector4f(0f, 0f, 1f, 1f))

        val vec3d3 = direction.mul(reachDistance.toFloat()) // multiply by look distance
            .let { Vec3d(it.x.toDouble(), it.y.toDouble(), it.z.toDouble()) }
            .let { origin.add(it) }

        val box = Box(
            origin.subtract(reachDistance, reachDistance, reachDistance),
            origin.add(reachDistance, reachDistance, reachDistance)
        )

        val blockDistanceSq = reachDistance.pow(2)

        val hit = ProjectileUtil.raycast(
            player.player,
            origin,
            vec3d3,
            box,
            { e -> tooltipService[e.uuid] != null },
            blockDistanceSq
        )

        val hitUuid = hit?.entity?.uuid
        val hitTooltip = hitUuid?.let { tooltipService[it] }

        if (hitUuid != null && hitTooltip != null) {
            val pair = player.uuid to hitUuid
            val ticks = prevTicks[pair] ?: 0
            lookingAtTicks[pair] = ticks + 1

            // only send tooltips once the player has been looking at the same entity for 1s
            if (ticks > 20) {
                val packet = TooltipPacket(hitTooltip)
                packets.tooltipV1.send(player.player, packet)
            }
        }
    }

    init {
        events.onGameTick {
            tick()
        }
    }
}
