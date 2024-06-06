@file:Suppress("unused")

package me.jfenn.bingo.common.commands

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import me.jfenn.bingo.common.commands.ChatCommand
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.test.BaseGameTest
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.server.MinecraftServer
import net.minecraft.server.PlayerManager
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext
import net.minecraft.text.Text
import java.util.*

class ChatCommandTest : BaseGameTest() {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun sendTeamMessageSendsAMessageToAPlayerOnTheSameTeam(context: TestContext) {
        val teamBlue = BingoTeam.BLUE.copy()

        val uuid1 = UUID.randomUUID()
        val mockPlayer1 = mockk<ServerPlayerEntity>()
        every { mockPlayer1.uuid } returns uuid1
        teamBlue.players.add(BingoPlayerProfile(uuid1, "player1"))

        val uuid2 = UUID.randomUUID()
        val mockPlayer2 = mockk<ServerPlayerEntity>()
        every { mockPlayer2.uuid } returns uuid2
        teamBlue.players.add(BingoPlayerProfile(uuid2, "player2"))

        val mockServer = mockk<MinecraftServer>()
        val mockPlayerManager = mockk<PlayerManager>()

        every { mockServer.playerManager } returns mockPlayerManager
        every { mockPlayerManager.playerList } returns listOf(mockPlayer1, mockPlayer2)

        every { mockPlayer1.displayName } returns Text.literal("Player 1")
        every { mockPlayer1.sendMessage(any()) } returns Unit

        every { mockPlayer2.sendMessage(any()) } returns Unit

        val message = Text.literal("Hi OwO")
        val expectedMessage = "TEAM Player 1: Hi OwO"
        ChatCommand.sendTeamMessage(mockPlayer1, teamBlue, message, mockServer)

        verify(exactly = 1) {
            mockPlayer1.sendMessage(match { it.string == expectedMessage })
        }

        verify(exactly = 1) {
            mockPlayer2.sendMessage(match { it.string == expectedMessage })
        }
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun sendTeamMessageSendsAMessageToAPlayerOnADifferentTeam(context: TestContext) {
        val teamBlue = BingoTeam.BLUE.copy()
        val teamGreen = BingoTeam.GREEN.copy()

        val uuidBlue = UUID.randomUUID()
        val mockPlayerBlue = mockk<ServerPlayerEntity>()
        every { mockPlayerBlue.uuid } returns uuidBlue
        teamBlue.players.add(BingoPlayerProfile(uuidBlue, "playerBlue"))

        val uuidGreen = UUID.randomUUID()
        val mockPlayerGreen = mockk<ServerPlayerEntity>()
        every { mockPlayerGreen.uuid } returns uuidGreen
        teamGreen.players.add(BingoPlayerProfile(uuidGreen, "playerGreen"))

        val mockServer = mockk<MinecraftServer>()
        val mockPlayerManager = mockk<PlayerManager>()

        every { mockServer.playerManager } returns mockPlayerManager
        every { mockPlayerManager.playerList } returns listOf(mockPlayerBlue, mockPlayerGreen)

        every { mockPlayerBlue.displayName } returns Text.literal("Player 1")
        every { mockPlayerBlue.sendMessage(any()) } returns Unit

        every { mockPlayerGreen.sendMessage(any()) } returns Unit

        val message = Text.literal("Hi OwO")
        val expectedMessage = "TEAM Player 1: Hi OwO"
        ChatCommand.sendTeamMessage(mockPlayerBlue, teamBlue, message, mockServer)

        verify(exactly = 1) {
            mockPlayerBlue.sendMessage(match { it.string == expectedMessage })
        }

        verify(exactly = 0) {
            mockPlayerGreen.sendMessage(any())
        }
    }
}