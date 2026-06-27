package me.jfenn.bingo.platform.player

import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.text.IText

interface IContainerMenu {
    interface Factory {
        fun createSimpleContainer(stacks: List<IItemStack>, size: Int = 9*3): IInventory
        fun createInventory(inventory: IInventory, name: IText, isInteractable: Boolean, onClose: () -> Unit): IContainerMenu
        fun createSpectatorInventory(inventory: IInventory, name: IText): IContainerMenu
    }
}