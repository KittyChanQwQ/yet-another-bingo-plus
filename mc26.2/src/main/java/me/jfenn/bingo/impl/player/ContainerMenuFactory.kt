package me.jfenn.bingo.impl.player

import me.jfenn.bingo.impl.inventory.InventoryImpl
import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.item.IItemStackFactory
import me.jfenn.bingo.platform.player.IContainerMenu
import me.jfenn.bingo.platform.text.IText
import net.minecraft.world.SimpleContainer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.inventory.MenuType
import kotlin.math.ceil

class ContainerMenuFactory(
    private val itemStackFactory: IItemStackFactory,
) : IContainerMenu.Factory {
    override fun createSimpleContainer(stacks: List<IItemStack>): IInventory {
        val container = SimpleContainer(stacks.size)
        stacks.forEachIndexed { i, stack ->
            container.setItem(i, stack.stack)
        }
        return InventoryImpl(itemStackFactory, container)
    }

    private fun getMenuType(rows: Int): MenuType<*> {
        return when (rows) {
            1 -> MenuType.GENERIC_9x1
            2 -> MenuType.GENERIC_9x2
            3 -> MenuType.GENERIC_9x3
            4 -> MenuType.GENERIC_9x4
            5 -> MenuType.GENERIC_9x5
            else -> MenuType.GENERIC_9x6
        }
    }

    override fun createInventory(
        inventory: IInventory,
        name: IText,
        isInteractable: Boolean,
        onClose: () -> Unit,
    ): IContainerMenu {
        val rows = ceil(inventory.size / 9.0).toInt()
        val type = getMenuType(rows)
        val inventory = when (inventory) {
            is InventoryImpl -> inventory.inventory
            else -> ContainerImpl(inventory)
        }

        val menuProvider = SimpleMenuProvider(
            { syncId, inv, _ ->
                ContainerScreenHandler(
                    type = type,
                    syncId = syncId,
                    rows = rows,
                    playerInventory = inv,
                    inventory = inventory,
                    isInteractable = isInteractable,
                    onClose = onClose,
                )
            },
            name.value,
        )
        return ContainerMenuImpl(menuProvider)
    }

    override fun createSpectatorInventory(inventory: IInventory, name: IText): IContainerMenu {
        val rows = ceil(inventory.size / 9.0).toInt()
        val type = getMenuType(rows)
        val inventory = when (inventory) {
            is InventoryImpl -> inventory.inventory
            else -> ContainerImpl(inventory)
        }

        val menuProvider = SimpleMenuProvider(
            { syncId, inv, _ ->
                SpectatorInventoryScreenHandler(
                    type = type,
                    syncId = syncId,
                    rows = rows,
                    playerInventory = inv,
                    inventory = inventory,
                )
            },
            name.value,
        )
        return ContainerMenuImpl(menuProvider)
    }
}

class ContainerMenuImpl(
    val menuProvider: SimpleMenuProvider,
) : IContainerMenu