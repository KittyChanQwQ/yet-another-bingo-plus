package me.jfenn.bingo.impl.player

import me.jfenn.bingo.api.annotations.BingoInternalApi
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class ContainerScreenHandler(
    type: MenuType<*>,
    syncId: Int,
    rows: Int,
    playerInventory: Inventory,
    inventory: Container,
    private val isInteractable: Boolean,
    private val onClose: () -> Unit,
): AbstractContainerMenu(type, syncId) {

    init {
        // chest inventory slots
        for (row in 0 until rows) {
            for (col in 0 until 9) {
                val index = col + row * 9
                val x = 8 + col * 18
                val y = 18 + row * 18

                this.addSlot(
                    if (isInteractable) {
                        Slot(inventory, index, x, y)
                    } else {
                        EmptySlot(inventory, index, x, y)
                    }
                )
            }
        }

        // player inventory -40 224
        val offset = rows*18 + 12
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                this.addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, offset + row * 18))
            }
        }

        // hotbar
        for (col in 0 until 9) {
            this.addSlot(Slot(playerInventory, col, 8 + col * 18, offset + 58))
        }
    }

    override fun moveItemStackTo(itemStack: ItemStack, startSlot: Int, endSlot: Int, backwards: Boolean): Boolean {
        return if (isInteractable) {
            super.moveItemStackTo(itemStack, startSlot, endSlot, backwards)
        } else false
    }

    override fun stillValid(player: Player): Boolean {
        return true
    }

    override fun slotsChanged(container: Container) {
        if (isInteractable) {
            super.slotsChanged(container)
        }
    }

    override fun quickMoveStack(player: Player, slotIndex: Int): ItemStack {
        if (!isInteractable) return ItemStack.EMPTY

        val slotInstance = slots[slotIndex]
        if (!slotInstance.hasItem())
            return ItemStack.EMPTY

        val originalStack = slotInstance.item
        val newStack = originalStack.copy()

        if (slotIndex < 3*9) {
            if (!moveItemStackTo(originalStack, 3*9, slots.size, true))
                return ItemStack.EMPTY
        } else {
            if (!moveItemStackTo(originalStack, 0, 3*9, false))
                return ItemStack.EMPTY
        }

        slotInstance.setChanged()
        return newStack
    }

    override fun clicked(slotIndex: Int, buttonNum: Int, containerInput: ContainerInput, player: Player) {
        if (isInteractable) {
            super.clicked(slotIndex, buttonNum, containerInput, player)
        }
    }

    @OptIn(BingoInternalApi::class)
    override fun removed(player: Player) {
        super.removed(player)
        onClose.invoke()
    }
}