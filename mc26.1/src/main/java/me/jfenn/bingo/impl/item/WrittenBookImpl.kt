package me.jfenn.bingo.impl.item

import me.jfenn.bingo.platform.item.IWrittenBook
import me.jfenn.bingo.platform.text.IText
import net.minecraft.core.component.DataComponents
import net.minecraft.server.network.Filterable
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.WrittenBookContent

class WrittenBookImpl(
    override val stack: ItemStack,
) : ItemStackImpl(stack), IWrittenBook {
    override var title: String?
        get() = stack[DataComponents.WRITTEN_BOOK_CONTENT]?.title?.raw
        set(value) {
            val original = stack[DataComponents.WRITTEN_BOOK_CONTENT]
            stack[DataComponents.WRITTEN_BOOK_CONTENT] = WrittenBookContent(
                value?.let { Filterable.passThrough(value) }
                    ?: Filterable.passThrough(""),
                original?.author ?: "",
                original?.generation ?: 0,
                original?.pages() ?: emptyList(),
                original?.resolved ?: true
            )
        }

    override var author: String?
        get() = stack[DataComponents.WRITTEN_BOOK_CONTENT]?.author
        set(value) {
            val original = stack[DataComponents.WRITTEN_BOOK_CONTENT]
            stack[DataComponents.WRITTEN_BOOK_CONTENT] = WrittenBookContent(
                original?.title ?: Filterable.passThrough(""),
                value ?: "",
                original?.generation ?: 0,
                original?.pages() ?: emptyList(),
                original?.resolved ?: true
            )
        }

    override fun setPages(pages: List<IText>) {
        val original = stack[DataComponents.WRITTEN_BOOK_CONTENT]
        stack[DataComponents.WRITTEN_BOOK_CONTENT] = WrittenBookContent(
            original?.title ?: Filterable.passThrough(""),
            original?.author ?: "",
            original?.generation ?: 0,
            pages.map { Filterable.passThrough(it.value) },
            original?.resolved ?: true
        )
    }
}