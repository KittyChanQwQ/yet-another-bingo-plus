package me.jfenn.bingo.impl

import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.api.item.IItemStackFactory
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.component.type.NbtComponent
import net.minecraft.component.type.UnbreakableComponent
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Unit

class ItemStackFactory : IItemStackFactory {
    override fun createStack(item: String, count: Int): IItemStack {
        return createStack(Identifier(item), count)
    }

    override fun createStack(item: Identifier, count: Int): IItemStack {
        return createStack(Registries.ITEM.get(item), count)
    }

    override fun createStack(item: Item, count: Int): IItemStack {
        return forStack(ItemStack(item, count))
    }

    override fun forStack(stack: ItemStack?): IItemStack {
        return ItemStackImpl(stack ?: ItemStack.EMPTY)
    }

    inner class ItemStackImpl(
        override val stack: ItemStack,
    ) : IItemStack {
        override val item: Item
            get() = stack.item

        override val identifier: Identifier
            get() = Registries.ITEM.getId(item)

        override val displayName: Text
            get() = stack.name

        override var count: Int
            get() = stack.count
            set(value) {
                stack.count = value
            }

        override val maxCount: Int
            get() = stack.maxCount

        override fun addCustomTag(tag: String) {
            val nbt = stack[DataComponentTypes.CUSTOM_DATA]?.copyNbt() ?: NbtCompound()
            nbt.putBoolean(tag, true)
            stack[DataComponentTypes.CUSTOM_DATA] = NbtComponent.of(nbt)
        }

        override fun removeCustomTag(tag: String) {
            val nbt = stack[DataComponentTypes.CUSTOM_DATA]?.copyNbt() ?: NbtCompound()
            nbt.remove(tag)
            stack[DataComponentTypes.CUSTOM_DATA] = NbtComponent.of(nbt)
        }

        override fun hasCustomTag(tag: String): Boolean {
            return stack[DataComponentTypes.CUSTOM_DATA]?.copyNbt()?.contains(tag) == true
        }

        override fun setDisplay(name: Text?, lore: List<Text>?) {
            stack[DataComponentTypes.CUSTOM_NAME] = name
            stack[DataComponentTypes.LORE] = lore?.let { LoreComponent(it) }
        }

        override fun setUnbreakable(value: Boolean) {
            stack[DataComponentTypes.UNBREAKABLE] = UnbreakableComponent(true)
        }

        override fun setHideFlags(hideFlags: Int) {
            // TODO: might need [ENCHANTMENTS].hideFlags = true?
            stack[DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP] = Unit.INSTANCE
        }

        override fun setNbtString(nbt: String?) {
            // TODO: I have no idea how to implement this
        }

        override fun getNbtString(): String? {
            return null
        }

        override fun copy(): IItemStack {
            return ItemStackImpl(stack.copy())
        }

    }
}
