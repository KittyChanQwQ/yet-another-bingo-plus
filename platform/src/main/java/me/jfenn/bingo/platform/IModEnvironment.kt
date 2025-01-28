package me.jfenn.bingo.platform

import java.nio.file.Path

interface IModEnvironment {
    val configDir: Path
    val gameDir: Path
    val envType: EnvType

    enum class EnvType {
        CLIENT,
        SERVER,
    }
}