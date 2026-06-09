package me.jfenn.bingo.impl.resource

import me.jfenn.bingo.impl.toMinecraftIdentifier
import me.jfenn.bingo.impl.toPlatformIdentifier
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.resource.IResourceFile
import me.jfenn.bingo.platform.resource.IResourceManager
import net.minecraft.resources.FileToIdConverter
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceManager
import java.io.InputStream
import kotlin.jvm.optionals.getOrNull

class ResourceManagerImpl(
    val manager: ResourceManager,
) : IResourceManager {
    override fun findJson(startingPath: String): Map<Identifier, IResourceFile> {
        return FileToIdConverter.json(startingPath)
            .listMatchingResources(manager)
            .map { (identifier, resource) ->
                identifier.toPlatformIdentifier() to ResourceFileImpl(resource)
            }
            .toMap()
    }

    override fun findResources(
        startingPath: String,
        callback: (Identifier) -> Boolean
    ): Map<Identifier, IResourceFile> {
        return manager.listResources(startingPath) { callback(it.toPlatformIdentifier()) }
            .map { (identifier, resource) -> identifier.toPlatformIdentifier() to ResourceFileImpl(resource) }
            .toMap()
    }

    override fun findAllResources(
        startingPath: String,
        callback: (Identifier) -> Boolean
    ): Map<Identifier, List<IResourceFile>> {
        return manager.listResourceStacks(startingPath) { callback(it.toPlatformIdentifier()) }
            .map { (identifier, stack) -> identifier.toPlatformIdentifier() to stack.map(::ResourceFileImpl) }
            .toMap()
    }

    override fun getResource(id: Identifier): IResourceFile? {
        return manager.getResource(id.toMinecraftIdentifier())
            .getOrNull()
            ?.let { ResourceFileImpl(it) }
    }
}

class ResourceFileImpl(
    val file: Resource,
) : IResourceFile {
    override val inputStream: InputStream
        get() = file.open()
}