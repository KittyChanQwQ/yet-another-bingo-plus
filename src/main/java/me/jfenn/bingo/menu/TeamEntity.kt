package me.jfenn.bingo.menu

import me.jfenn.bingo.Bingo
import me.jfenn.bingo.event.InteractionEntityEvents
import me.jfenn.bingo.minecraftServer
import me.jfenn.bingo.team.BingoTeam
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.decoration.InteractionEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class TeamEntity(
    val team: BingoTeam,
    val position: Vec3d,
    val world: World = Bingo.lobbyWorld,
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
            team.addPlayer(player)
        }
    }

}