package me.jfenn.bingo.platform

import java.nio.file.Path

interface ILevelStorage {
    fun getLevelSaveDir(worldId: Identifier): Path?
}