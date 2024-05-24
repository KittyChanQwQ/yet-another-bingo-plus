package me.jfenn.bingo.api

import net.minecraft.util.Identifier
import java.nio.file.Path

interface ILevelStorage {
    fun getLevelSaveDir(worldId: Identifier): Path?
}