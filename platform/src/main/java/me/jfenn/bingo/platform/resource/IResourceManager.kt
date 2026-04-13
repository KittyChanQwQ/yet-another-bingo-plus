package me.jfenn.bingo.platform.resource

import me.jfenn.bingo.platform.Identifier
import java.io.InputStream

interface IResourceManager {
    fun findJson(startingPath: String): Map<Identifier, IResourceFile>
    fun findResources(startingPath: String, callback: (Identifier) -> Boolean): Map<Identifier, IResourceFile>
    fun findAllResources(startingPath: String, callback: (Identifier) -> Boolean): Map<Identifier, List<IResourceFile>>
    fun getResource(id: Identifier): IResourceFile?
}

interface IResource {
    val path: String
}

interface IResourceFile {
    val inputStream: InputStream
}