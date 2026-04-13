package me.jfenn.bingo.common.card

import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.map.CardView
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.item.IItemStackFactory

class CardInventory(
    val view: CardView,
    val itemStackFactory: IItemStackFactory,
    val text: TextProvider,
) : IInventory {
    override val size: Int
        get() = 45

    private fun copyDisplay(tile: CardTile, stack: IItemStack) {
        stack.setDisplay(
            tile.name
                ?.takeIf { it != stack.displayName }
                ?.let { text.empty().append(it).resetStyle() },
            tile.lore
                .takeIf { it.isNotEmpty() }
                ?.map { text.empty().append(it).resetStyle() }
        )
    }

    fun createTileItem(tile: CardTile): IItemStack {
        if (tile.isHidden) {
            return itemStackFactory.emptyStack
        }

        val tileItem = tile.image.item
        val stack = when {
            tile.isAchieved -> runCatching { itemStackFactory.createStack("minecraft:lime_stained_glass_pane") }
                .getOrNull()
                ?.also { copyDisplay(tile, it) }
                ?: tileItem
            tile.isLocked -> runCatching { itemStackFactory.createStack("minecraft:barrier") }
                .getOrNull()
                ?.also { copyDisplay(tile, it) }
                ?: tileItem
            tileItem != null -> tileItem
            else -> {
                kotlin.runCatching { itemStackFactory.createStack("minecraft:knowledge_book") }
                    .getOrNull()
                    ?.also { copyDisplay(tile, it) }
            }
        }

        return stack ?: itemStackFactory.emptyStack
    }

    val inventory = List(45) { slot ->
        val slotX = (slot % 9) - 2
        val slotY = slot / 9

        if (slotX !in 0 until 5 || slotY !in 0 until 5)
            return@List itemStackFactory.emptyStack

        val tile = view.tiles.getOrNull(slotX + slotY * 5)
            ?: return@List itemStackFactory.emptyStack

        createTileItem(tile)
    }

    override fun get(index: Int): IItemStack? {
        return inventory.getOrNull(index)
    }

    override fun set(index: Int, stack: IItemStack) {}
}