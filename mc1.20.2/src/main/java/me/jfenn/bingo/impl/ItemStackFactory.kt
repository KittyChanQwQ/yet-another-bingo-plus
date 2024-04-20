package me.jfenn.bingo.impl

import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.api.ITextSerializer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class ItemStackFactory(
    private val textSerializer: ITextSerializer,
) : IItemStackFactory {
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
            stack.getOrCreateNbt().putBoolean(tag, true)
        }

        override fun removeCustomTag(tag: String) {
            stack.nbt?.remove(tag)
        }

        override fun hasCustomTag(tag: String): Boolean {
            return stack.nbt?.contains(tag) == true
        }

        override fun setDisplay(name: Text?, lore: List<Text>?) {
            stack.getOrCreateNbt().apply {
                if (name != null) {
                    putString("Name", textSerializer.toJson(name))
                }

                if (lore != null) {
                    put("Lore", NbtList().apply {
                        for (text in lore) {
                            add(NbtString.of(textSerializer.toJson(text)))
                        }
                    })
                }
            }
        }

        override fun setUnbreakable(value: Boolean) {
            stack.getOrCreateNbt().apply {
                putBoolean("Unbreakable", true)
            }
        }

        override fun setHideFlags(hideFlags: Int) {
            stack.getOrCreateNbt().apply {
                putInt("HideFlags", 255)
            }
        }

        override fun setNbtString(nbt: String?) {
            stack.nbt = StringNbtReader.parse(nbt)
        }

        override fun getNbtString(): String? {
            return stack.nbt?.toString()
        }

        override fun copy(): IItemStack {
            return ItemStackImpl(stack.copy())
        }

    }
}
