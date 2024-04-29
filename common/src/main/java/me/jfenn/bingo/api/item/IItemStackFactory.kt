package me.jfenn.bingo.api.item

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Identifier

interface IItemStackFactory {
    fun createStack(item: String, count: Int = 1): IItemStack
    fun createStack(item: Identifier, count: Int = 1): IItemStack
    fun createStack(item: Item, count: Int = 1): IItemStack
    fun forStack(stack: ItemStack?): IItemStack

    fun createFilledMap(): IFilledMap
    fun createFireworkRocket(): IFireworkRocket
    fun createWrittenBook(): IWrittenBook
}

interface IItemStack {
    val stack: ItemStack
    val item: Item
    val identifier: Identifier
    var count: Int
    val maxCount: Int

    val displayName: Text

    fun addCustomTag(tag: String)
    fun removeCustomTag(tag: String)
    fun hasCustomTag(tag: String): Boolean

    fun setDisplay(
        name: Text?,
        lore: List<Text>?,
    )

    fun setUnbreakable(value: Boolean)
    fun setHideFlags(hideFlags: Int)

    fun setNbtString(nbt: String?)
    fun getNbtString(): String?

    fun setComponentsString(components: Map<String, String?>)
    fun getComponentsString(): Map<String, String?>?

    fun copy(): IItemStack

    fun asFilledMap(): IFilledMap?
    fun asFireworkRocket(): IFireworkRocket?
    fun asWrittenBook(): IWrittenBook?
}