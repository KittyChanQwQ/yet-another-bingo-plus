package me.jfenn.bingo.utils

import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting

fun PlayerEntity.allHeldStacks() = sequence<ItemStack> {
    // yield all items in inventory
    for (i in 0 until inventory.size()) {
        yield(inventory.getStack(i))
    }

    // yield the current cursor stack
    yield(currentScreenHandler.cursorStack)
}

fun PlayerEntity.allInventorySlots() = sequence<Pair<Int, ItemStack>> {
    // yield all items in inventory
    for (i in 0 until inventory.size()) {
        yield(Pair(i, inventory.getStack(i)))
    }
}

/**
 * Give the player an ItemStack, equipping it in the preferred slot if armor,
 * and otherwise placing it in the inventory
 */
fun PlayerEntity.giveOrEquipStack(itemStack: ItemStack) {
    // if the item can be equipped (e.g. iron chestplate), put it in an equipment slot
    val slot = MobEntity.getPreferredEquipmentSlot(itemStack)
    if (canEquip(itemStack) && slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
        equipStack(slot, itemStack)
    } else {
        // otherwise, put it in the player's inventory
        giveItemStack(itemStack)
    }
}

fun PlayerEntity.sendInfoMessage(text: String) {
    sendMessage(
        Text.empty()
            .append(Text.literal("ℹ  ").formatted(Formatting.AQUA))
            .append(Text.literal(text).formatted(Formatting.AQUA, Formatting.ITALIC)),
        false,
    )
}

fun PlayerEntity.sendWarningMessage(text: String) {
    sendMessage(
        Text.empty()
            .append(Text.literal("⚠  ").formatted(Formatting.YELLOW))
            .append(Text.literal(text).formatted(Formatting.YELLOW, Formatting.ITALIC)),
        false,
    )
}

fun PlayerEntity.hasPermission(level: Int = 2): Boolean {
    return hasPermissionLevel(level) || server?.isSingleplayer == true
}
