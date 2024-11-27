@file:Suppress("unused")

package me.jfenn.bingo.common.commands

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.player.PlayerProfile
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.test.BaseGameTest
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext
import net.minecraft.text.Text
import java.util.*

class ChatCommandTest : BaseGameTest() {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun sendTeamMessageSendsAMessageToAPlayerOnTheSameTeam(context: TestContext) {
        val teamBlue = BingoTeam.BLUE.copy()

        val uuid1 = UUID.randomUUID()
        val mockPlayer1 = mockk<IPlayerHandle>()
        every { mockPlayer1.uuid } returns uuid1
        teamBlue.players.add(PlayerProfile(uuid1, "player1"))

        val uuid2 = UUID.randomUUID()
        val mockPlayer2 = mockk<IPlayerHandle>()
        every { mockPlayer2.uuid } returns uuid2
        teamBlue.players.add(PlayerProfile(uuid2, "player2"))

        val mockPlayerManager = mockk<IPlayerManager>()
        every { mockPlayerManager.getPlayers() } returns listOf(mockPlayer1, mockPlayer2)

        every { mockPlayer1.playerName } returns "Player 1"
        every { mockPlayer1.sendMessage(any()) } returns Unit

        every { mockPlayer2.sendMessage(any()) } returns Unit

        val message = Text.literal("Hi OwO")
        val expectedMessage = "TEAM Player 1: Hi OwO"
        ChatCommand.sendTeamMessage(mockPlayer1, teamBlue, message, mockPlayerManager)

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
        val mockPlayerBlue = mockk<IPlayerHandle>()
        every { mockPlayerBlue.uuid } returns uuidBlue
        teamBlue.players.add(PlayerProfile(uuidBlue, "playerBlue"))

        val uuidGreen = UUID.randomUUID()
        val mockPlayerGreen = mockk<IPlayerHandle>()
        every { mockPlayerGreen.uuid } returns uuidGreen
        teamGreen.players.add(PlayerProfile(uuidGreen, "playerGreen"))

        val mockPlayerManager = mockk<IPlayerManager>()
        every { mockPlayerManager.getPlayers() } returns listOf(mockPlayerBlue, mockPlayerGreen)

        every { mockPlayerBlue.playerName } returns "Player 1"
        every { mockPlayerBlue.sendMessage(any()) } returns Unit

        every { mockPlayerGreen.sendMessage(any()) } returns Unit

        val message = Text.literal("Hi OwO")
        val expectedMessage = "TEAM Player 1: Hi OwO"
        ChatCommand.sendTeamMessage(mockPlayerBlue, teamBlue, message, mockPlayerManager)

        verify(exactly = 1) {
            mockPlayerBlue.sendMessage(match { it.string == expectedMessage })
        }

        verify(exactly = 0) {
            mockPlayerGreen.sendMessage(any())
        }
    }
}