package me.jfenn.bingo.menu

import me.jfenn.bingo.event.InteractionEntityEvents
import me.jfenn.bingo.lobbyWorld
import me.jfenn.bingo.map.Color
import me.jfenn.bingo.minecraftServer
import me.jfenn.bingo.team.BingoTeam
import me.jfenn.bingo.utils.toNbt
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.decoration.InteractionEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.particle.DustParticleEffect
import net.minecraft.particle.ParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.joml.Vector3f

class TeamEntity(
    val team: BingoTeam,
    val position: Vec3d,
    val world: World = lobbyWorld,
) {

    private val interactionListenerEntity = InteractionEntity(EntityType.INTERACTION, world)
        .apply {
            readNbt(NbtCompound().apply {
                put("Pos", position.add(0.0, -1.0, 0.0).toNbt())
                putFloat("width", 2f)
                putFloat("height", 4f)
            })
        }
        .also { world.spawnEntity(it) }

    private val textDisplayEntity = DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world)
        .apply {
            readNbt(NbtCompound().apply {
                put("Pos", position.add(0.0, 1.5, 0.0).toNbt())
                putString("text", Text.Serializer.toJson(
                    Text.literal("${team.name} Team")
                        .formatted(team.formatting, Formatting.BOLD)
                ))
                putString("billboard", "vertical")
                putString("alignment", "center")
            })
        }
        .also { world.spawnEntity(it) }

    private val textDisplayEntity2 = DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world)
        .apply {
            readNbt(NbtCompound().apply {
                put("Pos", position.add(0.0, 1.0, 0.0).toNbt())
                putString("text", Text.Serializer.toJson(
                    Text.literal("Click to join!")
                ))
                putString("billboard", "vertical")
                putString("alignment", "center")
            })
        }
        .also { world.spawnEntity(it) }

    init {
        InteractionEntityEvents.onInteract(interactionListenerEntity) { player ->
            (player as? ServerPlayerEntity)?.let { player ->
                val color = Color.fromInt(team.formatting.colorValue ?: 0)
                val effect = DustParticleEffect(Vector3f(color.r / 255f, color.g / 255f, color.b / 255f), 1f)
                (world as? ServerWorld)?.spawnParticles(player, effect, true, position.x, position.y, position.z, 10, 1.0, 1.0, 1.0, 0.2)

                player.networkHandler.sendPacket(
                    PlaySoundS2CPacket(
                        Registries.SOUND_EVENT.getEntry(SoundEvents.ITEM_LODESTONE_COMPASS_LOCK),
                        SoundCategory.BLOCKS,
                        player.x, player.y, player.z,
                        1f, 1f, 0
                    )
                )
            }

            team.addPlayer(player)
        }
    }

}