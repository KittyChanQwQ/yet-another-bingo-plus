package me.jfenn.bingo.impl

import me.jfenn.bingo.platform.IRegistryEntry
import me.jfenn.bingo.platform.ITagAccessor
import me.jfenn.bingo.platform.ITagContents
import net.minecraft.block.Block
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import net.minecraft.world.biome.Biome
import kotlin.jvm.optionals.getOrNull

class TagAccessorImpl : ITagAccessor {
    override fun getItemTag(id: String): List<String>? {
        val tag = try {
            TagKey.of(RegistryKeys.ITEM, Identifier(id))
        } catch (e: IllegalArgumentException) {
            return null
        }

        return Registries.ITEM.getEntryList(tag).getOrNull()
            ?.map { it.idAsString }
    }

    override fun getBlockTag(id: String): ITagContents<IRegistryEntry.Block> {
        val tagKey = TagKey.of(RegistryKeys.BLOCK, Identifier(id))
        return TagContentsImpl(tagKey)
    }

    override fun getBiomeTag(id: String): ITagContents<IRegistryEntry.Biome> {
        val tagKey = TagKey.of(RegistryKeys.BIOME, Identifier(id))
        return TagContentsImpl(tagKey)
    }
}

class TagContentsImpl<T: IRegistryEntry>(
    private val tagKey: TagKey<*>,
) : ITagContents<T> {
    override fun contains(entry: T): Boolean {
        return when (entry) {
            is BlockRegistryEntry -> entry.entry.isIn(tagKey as TagKey<Block>)
            is BiomeRegistryEntry -> entry.entry.isIn(tagKey as TagKey<Biome>)
            else -> error("Unknown key class ${entry::class.java.simpleName}")
        }
    }
}

class BlockRegistryEntry(
    val entry: RegistryEntry<Block>
): IRegistryEntry.Block

class BiomeRegistryEntry(
    val entry: RegistryEntry<Biome>
): IRegistryEntry.Biome
