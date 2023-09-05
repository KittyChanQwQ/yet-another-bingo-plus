package me.jfenn.bingo.menu

import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.card.TeamService
import me.jfenn.bingo.lobbyWorld
import me.jfenn.bingo.utils.sendInfoMessage
import me.jfenn.bingo.utils.toNbt
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class TeamsEntity(
    private val world: World = lobbyWorld,
    private val position: Vec3d,
    private val requiredPermissionLevel: Int = 2,
    private val title: String = "Teams",
) {

    private var teamCount = 3
        set(value) {
            field = value
            update()
        }

    val incrementButton = ButtonEntity(
        world = world,
        position = position.add(0.0, -MENU_ITEM_OFFSET, 0.0),
        text = "+",
        requiredPermissionLevel = requiredPermissionLevel,
    ).apply {
        onClick {
            teamCount = (teamCount + 1).coerceIn(1, BingoTeam.values().size)
        }
    }

    val decrementButton = ButtonEntity(
        world = world,
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 2 * MENU_ITEM_HEIGHT), 0.0),
        text = "-",
        requiredPermissionLevel = requiredPermissionLevel,
    ).apply {
        onClick {
            teamCount = (teamCount - 1).coerceIn(1, BingoTeam.values().size)
        }
    }

    val textEntity = DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world)
        .also { world.spawnEntity(it) }

    val shuffleButton = ButtonEntity(
        world = world,
        position = position.add(0.0, -(MENU_ITEM_OFFSET + 3 * MENU_ITEM_HEIGHT), 0.0),
        text = "Shuffle!",
        requiredPermissionLevel = requiredPermissionLevel,
    ).apply {
        onClick { player ->
            TeamService.shuffleTeams(teamCount)
            player.sendInfoMessage("Teams have been shuffled!")
        }
    }

    init {
        DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world)
            .apply {
                readNbt(NbtCompound().apply {
                    put("Pos", position.toNbt())
                    putString("text", Text.Serializer.toJson(
                        Text.literal(title)
                    ))
                    putString("billboard", "fixed")
                    putString("alignment", "center")
                    putInt("background", 0)
                    putBoolean("shadow", true)
                })
            }
            .also { world.spawnEntity(it) }

        update()
    }

    private fun update() {
        textEntity.readNbt(NbtCompound().apply {
            put("Pos", position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0).toNbt())
            putString("text", Text.Serializer.toJson(
                Text.literal("$teamCount teams")
            ))
            putString("billboard", "fixed")
            putString("alignment", "center")
            putInt("background", 0)
            putBoolean("shadow", true)
        })
    }

}