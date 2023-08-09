package me.jfenn.bingo.menu

import me.jfenn.bingo.Bingo
import me.jfenn.bingo.minecraftServer
import me.jfenn.bingo.utils.EventListener
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class ItemDistributionEntity(
    private val world: World = Bingo.lobbyWorld,
    private val position: Vec3d,
    private var itemDistribution: List<Int>,
    private val requiredPermissionLevel: Int = 2,
) {

    private val labels = listOf("A", "B", "C", "D", "S")

    private val tierIncrementButtons = List(5) { i ->
        ButtonEntity(
            world = world,
            position = position.add(i - 2.0, -1.0, 0.0),
            text = "+",
            requiredPermissionLevel = requiredPermissionLevel,
        ).apply {
            onClick {
                itemDistribution = itemDistribution.toMutableList().apply {
                    set(i, (itemDistribution[i] + 1).coerceAtMost(25))
                }
                update()
                onChange(itemDistribution)
            }
        }
    }

    private val tierDecrementButtons = List(5) { i ->
        ButtonEntity(
            world = world,
            position = position.add(i - 2.0, -2.0, 0.0),
            text = "-",
            requiredPermissionLevel = requiredPermissionLevel,
        ).apply {
            onClick {
                itemDistribution = itemDistribution.toMutableList().apply {
                    set(i, (itemDistribution[i] - 1).coerceAtLeast(0))
                }
                update()
                onChange(itemDistribution)
            }
        }
    }

    private val tierDisplayEntities = List(5) { i ->
        DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world)
            .also { world.spawnEntity(it) }
    }

    val onChange = EventListener<List<Int>>()

    init {
        update()

        DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world)
            .also { world.spawnEntity(it) }
            .apply {
                readNbt(NbtCompound().apply {
                    put("Pos", position.toNbt())
                    putString("text", Text.Serializer.toJson(
                        Text.literal("Item Distribution")
                    ))
                    putString("billboard", "fixed")
                    putString("alignment", "center")
                })
            }
    }

    private fun update() {
        for (i in labels.indices) {
            val tierDisplayEntity = tierDisplayEntities[i]

            tierDisplayEntity.readNbt(NbtCompound().apply {
                put("Pos", position.add(i - 2.0, -1.5, 0.0).toNbt())
                putString("text", Text.Serializer.toJson(
                    Text.literal(itemDistribution[i].toString())
                ))
                putString("billboard", "fixed")
                putString("alignment", "center")
            })
        }
    }

}