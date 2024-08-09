package me.jfenn.bingo.impl

import me.jfenn.bingo.api.ITagAccessor
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import kotlin.jvm.optionals.getOrNull

class TagAccessorImpl : ITagAccessor {
    override fun getItemTag(id: String): List<String>? {
        val tag = try {
            TagKey.of(RegistryKeys.ITEM, Identifier.of(id))
        } catch (e: IllegalArgumentException) {
            return null
        }

        return Registries.ITEM.getEntryList(tag).getOrNull()
            ?.map { it.idAsString }
    }
}