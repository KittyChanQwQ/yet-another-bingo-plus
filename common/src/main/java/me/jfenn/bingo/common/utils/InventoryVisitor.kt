package me.jfenn.bingo.common.utils

import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.item.IItemStack

fun IPlayerHandle.allHeldStacks(): Sequence<IItemStack> = sequence {
    fun allInventoryStacks(inventory: IInventory): Sequence<IItemStack> = sequence {
        for (stack in inventory) {
            yield(stack)
            stack.bundleInventory?.let {
                yieldAll(allInventoryStacks(it))
            }
            stack.containerInventory?.let {
                yieldAll(allInventoryStacks(it))
            }
        }
    }

    for (inventory in allInventories) {
        yieldAll(allInventoryStacks(inventory))
    }
}

enum class InventoryVisitor { SKIP, MODIFY, BREAK }

fun visitInventories(
    inventories: List<IInventory>,
    visitor: (IItemStack) -> InventoryVisitor
) {
    var isBroken = false

    fun processInventory(inventory: IInventory): Boolean {
        if (isBroken) return false

        var isDirty = false
        for (index in inventory.indices) {
            val stack = inventory[index] ?: continue
            val action = visitor(stack)

            stack.bundleInventory?.let { bundle ->
                if (processInventory(bundle)) {
                    stack.setBundleInventory(bundle)
                    isDirty = true
                }
            }

            stack.containerInventory?.let { container ->
                if (processInventory(container)) {
                    stack.setContainerInventory(container)
                    isDirty = true
                }
            }

            when (action) {
                InventoryVisitor.SKIP -> continue
                InventoryVisitor.MODIFY -> {
                    inventory[index] = stack
                    isDirty = true
                }
                InventoryVisitor.BREAK -> {
                    inventory[index] = stack
                    isBroken = true
                    return true
                }
            }
        }

        return isDirty
    }

    inventories.forEach { processInventory(it) }
}
