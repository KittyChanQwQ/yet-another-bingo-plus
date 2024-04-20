package me.jfenn.bingo.impl.item

import me.jfenn.bingo.api.ITextSerializer
import me.jfenn.bingo.api.item.IWrittenBook
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.text.Text

class WrittenBookImpl(
    override val stack: ItemStack,
    private val textSerializer: ITextSerializer,
) : ItemStackFactory.ItemStackImpl(stack), IWrittenBook {
    override var title: String?
        get() = stack.nbt?.getString("title")
        set(value) {
            when {
                value != null -> stack.getOrCreateNbt().putString("title", value)
                else -> stack.nbt?.remove("title")
            }
        }

    override var author: String?
        get() = stack.nbt?.getString("author")
        set(value) {
            when {
                value != null -> stack.getOrCreateNbt().putString("author", value)
                else -> stack.nbt?.remove("author")
            }
        }

    override fun setPages(pages: List<Text>) {
        stack.getOrCreateNbt().apply {
            put("pages", NbtList().apply {
                for (page in pages) {
                    add(NbtString.of(textSerializer.toJson(page)))
                }
            })
        }
    }
}