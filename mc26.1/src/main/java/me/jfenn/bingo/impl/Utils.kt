package me.jfenn.bingo.impl

import me.jfenn.bingo.platform.Hand
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.block.ChunkPosition
import me.jfenn.bingo.platform.text.Formatting
import net.minecraft.ChatFormatting
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.ChunkPos
import net.minecraft.resources.Identifier as MinecraftIdentifier

fun Identifier.toMinecraftIdentifier(): MinecraftIdentifier =
    MinecraftIdentifier.fromNamespaceAndPath(namespace, path)

fun MinecraftIdentifier.toPlatformIdentifier(): Identifier =
    Identifier.of(namespace, path)

fun Hand.toInteractionHand() = when (this) {
    Hand.MAIN_HAND -> InteractionHand.MAIN_HAND
    Hand.OFF_HAND -> InteractionHand.OFF_HAND
}

fun InteractionHand.toPlatformHand() = when (this) {
    InteractionHand.MAIN_HAND -> Hand.MAIN_HAND
    InteractionHand.OFF_HAND -> Hand.OFF_HAND
}

fun Formatting.toMinecraftFormatting(): ChatFormatting = ChatFormatting.valueOf(this.name)
fun ChatFormatting.toPlatformFormatting(): Formatting = Formatting.valueOf(this.name)

fun ChunkPosition.toChunkPos() = ChunkPos(x, z)
