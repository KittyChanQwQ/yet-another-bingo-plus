package me.jfenn.bingo.platform.config

import java.nio.file.Path
import kotlin.reflect.KType

interface IConfigManager {
    val configDir: Path
    fun <T: Any> read(type: KType, file: String): T
    fun <T: Any> write(type: KType, file: String, config: T)
}

inline fun <reified T: Any> IConfigManager.read(file: String): T =
    read(kotlin.reflect.typeOf<T>(), file)

inline fun <reified T: Any> IConfigManager.write(file: String, config: T) =
    write(kotlin.reflect.typeOf<T>(), file, config)
