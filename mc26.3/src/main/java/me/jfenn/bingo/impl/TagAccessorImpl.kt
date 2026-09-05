package me.jfenn.bingo.impl

import me.jfenn.bingo.platform.IRegistryEntry
import me.jfenn.bingo.platform.ITagAccessor
import me.jfenn.bingo.platform.ITagContents
import me.jfenn.bingo.platform.text.IText
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.tags.TagKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.*
import kotlin.jvm.optionals.getOrNull

class TagAccessorImpl(
    private val server: MinecraftServer,
) : ITagAccessor {
    override fun getItemTag(id: String): List<String>? {
        val tag = try {
            TagKey.create(Registries.ITEM, Identifier.parse(id))
        } catch (e: IllegalArgumentException) {
            return null
        }

        return BuiltInRegistries.ITEM.get(tag).getOrNull()
            ?.map { it.registeredName }
    }

    override fun getBlockTag(id: String): ITagContents<IRegistryEntry.Block> {
        val tagKey = TagKey.create(Registries.BLOCK, Identifier.parse(id))
        return BlockTagContentsImpl(tagKey)
    }

    override fun getBiomeTag(id: String): ITagContents<IRegistryEntry.Biome> {
        val registry = server.registryAccess().lookupOrThrow(Registries.BIOME)
        val tagKey = TagKey.create(Registries.BIOME, Identifier.parse(id))
        return BiomeTagContentsImpl(registry, tagKey)
    }
}

abstract class TagContentsImpl<T: IRegistryEntry, U: Any>(
    private val tagKey: TagKey<U>,
) : ITagContents<T> {
    abstract val registry: Registry<U>
    abstract fun toMinecraftEntry(entry: T): Holder<U>
    abstract fun toPlatformEntry(entry: Holder<U>): T

    override fun list(): List<T> {
        return registry.get(tagKey)
            .getOrNull()
            ?.map { toPlatformEntry(it) }
            .orEmpty()
    }

    override fun contains(entry: T): Boolean {
        return toMinecraftEntry(entry).`is`(tagKey)
    }
}

class BlockTagContentsImpl(
    tagKey: TagKey<Block>,
): TagContentsImpl<IRegistryEntry.Block, Block>(tagKey) {
    override val registry: Registry<Block> = BuiltInRegistries.BLOCK
    override fun toMinecraftEntry(entry: IRegistryEntry.Block): Holder<Block> =
        (entry as BlockRegistryEntry).entry
    override fun toPlatformEntry(entry: Holder<Block>): BlockRegistryEntry =
        BlockRegistryEntry(entry)
}

class BlockRegistryEntry(
    val entry: Holder<Block>
): IRegistryEntry.Block {
    override val isDoorBlock: Boolean
        get() = entry.value() is DoorBlock || entry.value() is TrapDoorBlock
    override val isChestLike: Boolean
        get() = entry.value() is ChestBlock || entry.value() is BarrelBlock || entry.value() is ShulkerBoxBlock
    override val name: IText
        get() = TextImpl(entry.value().name.copy())
}

class BiomeTagContentsImpl(
    override val registry: Registry<Biome>,
    tagKey: TagKey<Biome>,
): TagContentsImpl<IRegistryEntry.Biome, Biome>(tagKey) {
    override fun toMinecraftEntry(entry: IRegistryEntry.Biome): Holder<Biome> =
        (entry as BiomeRegistryEntry).entry
    override fun toPlatformEntry(entry: Holder<Biome>): BiomeRegistryEntry =
        BiomeRegistryEntry(entry)
}

class BiomeRegistryEntry(
    val entry: Holder<Biome>
): IRegistryEntry.Biome
