package me.jfenn.bingo.impl

import me.jfenn.bingo.api.item.*
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.*
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.text.RawFilteredPair
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Unit

class ItemStackFactory : IItemStackFactory {
    override fun createStack(item: String, count: Int): IItemStack {
        return createStack(Identifier(item), count)
    }

    override fun createStack(item: Identifier, count: Int): IItemStack {
        val itemInstance = Registries.ITEM.get(item)
        if (itemInstance == Items.AIR && !item.path.equals("air", ignoreCase = true))
            throw IllegalArgumentException("[ItemStackFactory] Item $item not found")
        return createStack(itemInstance, count)
    }

    override fun createStack(item: Item, count: Int): IItemStack {
        return forStack(ItemStack(item, count))
    }

    override fun forStack(stack: ItemStack?): IItemStack {
        return ItemStackImpl(stack ?: ItemStack.EMPTY)
    }

    override fun createFilledMap(): IFilledMap = createStack(Items.FILLED_MAP, 1).asFilledMap()!!

    override fun createFireworkRocket(): IFireworkRocket = createStack(Items.FIREWORK_ROCKET, 1).asFireworkRocket()!!

    override fun createWrittenBook(): IWrittenBook = createStack(Items.WRITTEN_BOOK, 1).asWrittenBook()!!

    open inner class ItemStackImpl(
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

        override fun asFilledMap(): IFilledMap? {
            return (this as? FilledMapImpl)
                ?: takeIf { item == Items.FILLED_MAP }
                    ?.let { FilledMapImpl(stack) }
        }

        override fun asFireworkRocket(): IFireworkRocket? {
            return (this as? FireworkRocketImpl)
                ?: takeIf { item == Items.FIREWORK_ROCKET }
                    ?.let { FireworkRocketImpl(stack) }
        }

        override fun asWrittenBook(): IWrittenBook? {
            return (this as? WrittenBookImpl)
                ?: takeIf { item == Items.WRITTEN_BOOK }
                    ?.let { WrittenBookImpl(stack) }
        }

    }

    inner class WrittenBookImpl(
        override val stack: ItemStack,
    ) : ItemStackImpl(stack), IWrittenBook {
        override var title: String?
            get() = stack[DataComponentTypes.WRITTEN_BOOK_CONTENT]?.title?.raw
            set(value) {
                val original = stack[DataComponentTypes.WRITTEN_BOOK_CONTENT]
                stack[DataComponentTypes.WRITTEN_BOOK_CONTENT] = WrittenBookContentComponent(
                    value?.let { RawFilteredPair.of(value) }
                        ?: RawFilteredPair.of(""),
                    original?.author ?: "",
                    original?.generation ?: 0,
                    original?.pages ?: emptyList(),
                    original?.resolved ?: true
                )
            }

        override var author: String?
            get() = stack[DataComponentTypes.WRITTEN_BOOK_CONTENT]?.author
            set(value) {
                val original = stack[DataComponentTypes.WRITTEN_BOOK_CONTENT]
                stack[DataComponentTypes.WRITTEN_BOOK_CONTENT] = WrittenBookContentComponent(
                    original?.title ?: RawFilteredPair.of(""),
                    value ?: "",
                    original?.generation ?: 0,
                    original?.pages ?: emptyList(),
                    original?.resolved ?: true
                )
            }

        override fun setPages(pages: List<Text>) {
            val original = stack[DataComponentTypes.WRITTEN_BOOK_CONTENT]
            stack[DataComponentTypes.WRITTEN_BOOK_CONTENT] = WrittenBookContentComponent(
                original?.title ?: RawFilteredPair.of(""),
                original?.author ?: "",
                original?.generation ?: 0,
                pages.map { RawFilteredPair.of(it) },
                original?.resolved ?: true
            )
        }
    }

    inner class FilledMapImpl(
        override val stack: ItemStack,
    ) : ItemStackImpl(stack), IFilledMap {
        override var mapId: Int?
            get() = stack[DataComponentTypes.MAP_ID]?.id
            set(value) {
                stack[DataComponentTypes.MAP_ID] = value?.let { MapIdComponent(it) }
            }

        override var mapColor: Int?
            get() = stack[DataComponentTypes.MAP_COLOR]?.rgb
            set(value) {
                stack[DataComponentTypes.MAP_COLOR] = value?.let { MapColorComponent(it) }
            }
    }

    inner class FireworkRocketImpl(
        override val stack: ItemStack,
    ) : ItemStackImpl(stack), IFireworkRocket {
        override var fireworks: List<kotlin.Unit>?
            get() = emptyList()
            set(value) {
                stack[DataComponentTypes.FIREWORKS] = value?.let { FireworksComponent(0, emptyList()) }
            }
    }
}
