package me.jfenn.bingo.impl.item

import me.jfenn.bingo.impl.TextImpl
import me.jfenn.bingo.impl.inventory.ListInventoryImpl
import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.item.*
import me.jfenn.bingo.platform.text.IText
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier
import net.minecraft.util.Unit
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.*

open class ItemStackImpl(
    override val stack: ItemStack,
) : IItemStack {
    override val item: Item
        get() = stack.item

    override val identifier: Identifier
        get() = BuiltInRegistries.ITEM.getKey(item)

    override val displayName: IText
        get() = TextImpl(stack.hoverName.copy())

    override val lore: List<IText>
        get() = stack[DataComponents.LORE]?.lines?.map { TextImpl(it.copy()) } ?: emptyList()

    override var count: Int
        get() = stack.count
        set(value) {
            stack.count = value
        }

    override val maxCount: Int
        get() = stack.maxStackSize

    override fun addCustomTag(tag: String) {
        val nbt = stack[DataComponents.CUSTOM_DATA]?.copyTag() ?: CompoundTag()
        nbt.putBoolean(tag, true)
        stack[DataComponents.CUSTOM_DATA] = CustomData.of(nbt)
    }

    override fun removeCustomTag(tag: String) {
        val nbt = stack[DataComponents.CUSTOM_DATA]?.copyTag() ?: CompoundTag()
        nbt.remove(tag)
        if (nbt.isEmpty) {
            stack.remove(DataComponents.CUSTOM_DATA)
        } else {
            stack[DataComponents.CUSTOM_DATA] = CustomData.of(nbt)
        }
    }

    override fun hasCustomTag(tag: String): Boolean {
        return stack[DataComponents.CUSTOM_DATA]?.copyTag()?.contains(tag) == true
    }

    override fun setDisplay(name: IText?, lore: List<IText>?) {
        stack[DataComponents.CUSTOM_NAME] = name?.value
        stack[DataComponents.LORE] = lore?.let { ItemLore(it.map { t -> t.value }) }
    }

    override fun setUnbreakable(value: Boolean) {
        stack[DataComponents.UNBREAKABLE] = Unit.INSTANCE
    }

    override fun setHideFlags(hideFlags: Int) {
        stack[DataComponents.TOOLTIP_DISPLAY] = TooltipDisplay(
            false,
            LinkedHashSet<DataComponentType<*>>().apply {
                add(DataComponents.ENCHANTMENTS)
                add(DataComponents.MAP_ID)
                add(DataComponents.WRITTEN_BOOK_CONTENT)
                add(DataComponents.ATTRIBUTE_MODIFIERS)
            }
        )
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

    override fun asPlayerHead(): IPlayerHead? {
        return (this as? PlayerHeadImpl)
            ?: takeIf { item == Items.PLAYER_HEAD }
                ?.let { PlayerHeadImpl(stack) }
    }

    override val bundleInventory: IInventory?
        get() = stack[DataComponents.BUNDLE_CONTENTS]
            ?.items()
            ?.map { ItemStackFactory.forStack(it.create()) }
            .orEmpty()
            .let { ListInventoryImpl(it) }

    override fun setBundleInventory(inventory: Collection<IItemStack>) {
        stack[DataComponents.BUNDLE_CONTENTS] = BundleContents(
            inventory.filter { !it.isEmpty }
                .map { ItemStackTemplate.fromNonEmptyStack(it.stack) }
        )
    }

    override val containerInventory: IInventory?
        get() = stack[DataComponents.CONTAINER]
            ?.allItemsCopyStream()
            ?.map { ItemStackFactory.forStack(it) }
            ?.toList()
            .orEmpty()
            .let { ListInventoryImpl(it) }

    override fun setContainerInventory(inventory: Collection<IItemStack>) {
        stack[DataComponents.CONTAINER] = ItemContainerContents.fromItems(
            inventory.map { it.stack }
        )
    }

    override fun equals(other: Any?): Boolean = other is ItemStackImpl && stack == other.stack
    override fun hashCode(): Int = stack.hashCode()
}