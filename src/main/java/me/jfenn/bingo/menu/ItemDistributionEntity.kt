package me.jfenn.bingo.menu

import me.jfenn.bingo.card.GeneratorTier
import me.jfenn.bingo.lobbyWorld
import me.jfenn.bingo.utils.EventListener
import me.jfenn.bingo.utils.toNbt
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class ItemDistributionEntity(
    private val world: World = lobbyWorld,
    private val position: Vec3d,
    private var itemDistribution: List<Int>,
    private val requiredPermissionLevel: Int = 2,
    private val tags: Set<String> = emptySet(),
) {

    companion object {
        const val TIER_WIDTH = 0.5
        const val TIER_OFFSET = -1.0
    }

    private val tierIncrementButtons = List(5) { i ->
        ButtonEntity(
            world = world,
            position = position.add(i*TIER_WIDTH + TIER_OFFSET, -MENU_ITEM_OFFSET, 0.0),
            text = "+",
            requiredPermissionLevel = requiredPermissionLevel,
            width = TIER_WIDTH.toFloat(),
            tags = tags,
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
            position = position.add(i*TIER_WIDTH + TIER_OFFSET, -(MENU_ITEM_OFFSET + 3*MENU_ITEM_HEIGHT), 0.0),
            text = "-",
            requiredPermissionLevel = requiredPermissionLevel,
            width = TIER_WIDTH.toFloat(),
            tags = tags,
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
                    putInt("background", 0)
                    putBoolean("shadow", true)
                    put("Tags", NbtList().apply {
                        tags.forEach { add(NbtString.of(it)) }
                    })
                })
            }

        for (i in GeneratorTier.values().indices) {
            val tier = GeneratorTier.values()[i]

            DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world)
                .apply {
                    readNbt(NbtCompound().apply {
                        put("Pos", position.add(i*TIER_WIDTH + TIER_OFFSET, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0).toNbt())
                        putString("text", Text.Serializer.toJson(
                            Text.literal(tier.name).formatted(tier.formatting, Formatting.BOLD)
                        ))
                        putString("billboard", "fixed")
                        putString("alignment", "center")
                        putInt("background", 0)
                        putBoolean("shadow", true)
                        put("Tags", NbtList().apply {
                            tags.forEach { add(NbtString.of(it)) }
                        })
                    })
                }
                .also { world.spawnEntity(it) }
        }
    }

    private fun update() {
        val isValid = itemDistribution.sum() == 25

        for (i in GeneratorTier.values().indices) {
            val tierDisplayEntity = tierDisplayEntities[i]

            tierDisplayEntity.readNbt(NbtCompound().apply {
                put("Pos", position.add(i*TIER_WIDTH + TIER_OFFSET, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0).toNbt())
                putString("text", Text.Serializer.toJson(
                    Text.literal(itemDistribution[i].toString())
                        .formatted(if (isValid) Formatting.WHITE else Formatting.RED)
                ))
                putString("billboard", "fixed")
                putString("alignment", "center")
                putInt("background", 0)
                putBoolean("shadow", true)
                put("Tags", NbtList().apply {
                    tags.forEach { add(NbtString.of(it)) }
                })
            })
        }
    }

}