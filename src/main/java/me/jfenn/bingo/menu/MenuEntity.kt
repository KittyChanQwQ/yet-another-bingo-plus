package me.jfenn.bingo.menu

import me.jfenn.bingo.lobbyWorld
import me.jfenn.bingo.utils.EventListener
import me.jfenn.bingo.utils.toNbt
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class MenuEntity(
    world: World = lobbyWorld,
    title: String,
    private val options: List<String>,
    private val position: Vec3d,
    selectedIndex: Int = 0,
    requiredPermissionLevel: Int = 2,
) {

    private val buttonEntities = List(options.size) { i ->
        ButtonEntity(
            world = world,
            position = position.add(0.0, -(MENU_ITEM_OFFSET + i * MENU_ITEM_HEIGHT), 0.0),
            text = options[i],
            requiredPermissionLevel = requiredPermissionLevel,
        ).apply {
            onClick {
                this@MenuEntity.selectedIndex = i
            }
        }
    }

    val onChange = EventListener<Int>()

    var selectedIndex: Int = selectedIndex
        set(value) {
            field = value
            update()
            onChange(value)
        }

    init {
        update()

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
    }

    private fun update() {
        for (i in options.indices) {
            buttonEntities[i].isActive = selectedIndex == i
        }
    }
}