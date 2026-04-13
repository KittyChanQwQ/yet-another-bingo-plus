package me.jfenn.bingo.common.spawn

import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.NBT_BINGO_KEEP
import me.jfenn.bingo.common.NBT_BINGO_VANISH
import me.jfenn.bingo.common.utils.allHeldStacks
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.item.IItemStackFactory

class ElytraService(
    private val itemStackFactory: IItemStackFactory,
) {

    /**
     * Equip the player with elytra and firework rockets.
     * Only runs if config.isElytra is true
     */
    fun giveElytra(player: IPlayerHandle) {
        val hasElytra = player.allHeldStacks()
            .any { it.identifier.toString() == "minecraft:elytra" && it.hasCustomTag(NBT_BINGO_VANISH) }

        if (!hasElytra) {
            // insert elytra in armor slot
            player.giveOrEquipStack(
                itemStackFactory.createStack("minecraft:elytra").apply {
                    // unbreakable (should not take damage)
                    setUnbreakable(true)
                    addCustomTag(NBT_BINGO_IGNORE)
                    addCustomTag(NBT_BINGO_VANISH)
                    addCustomTag(NBT_BINGO_KEEP)
                }
            )
        }

        // also give rocket
        val rocketStack = player.allHeldStacks()
            .find {
                it.asFireworkRocket() != null && it.hasCustomTag(NBT_BINGO_VANISH)
            }

        if (rocketStack != null && rocketStack.count < 2) {
            rocketStack.count = 2
        }

        if (rocketStack == null) {
            player.giveItemStack(
                itemStackFactory.createFireworkRocket().apply {
                    addCustomTag(NBT_BINGO_IGNORE)
                    addCustomTag(NBT_BINGO_VANISH)
                    addCustomTag(NBT_BINGO_KEEP)
                    fireworks = emptyList()
                }
            )
        }
    }

    fun takeElytra(player: IPlayerHandle) {
        player.allHeldStacks()
            .filter { it.identifier.toString() == "minecraft:elytra" || it.asFireworkRocket() != null }
            .filter { it.hasCustomTag(NBT_BINGO_VANISH) }
            .forEach {
                it.count = 0
            }
    }

}