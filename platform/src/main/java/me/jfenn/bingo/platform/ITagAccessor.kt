package me.jfenn.bingo.platform

import me.jfenn.bingo.platform.text.IText

interface ITagAccessor {

    fun getItemTag(id: String): List<String>?

    fun getBlockTag(id: String): ITagContents<IRegistryEntry.Block>

    fun getBiomeTag(id: String): ITagContents<IRegistryEntry.Biome>

}

interface ITagContents<T: IRegistryEntry> {
    fun list(): List<T>
    fun contains(entry: T): Boolean
}

interface IRegistryEntry {
    interface Block : IRegistryEntry {
        val isDoorBlock: Boolean
        val isChestLike: Boolean
        val name: IText
    }
    interface Biome : IRegistryEntry
}
