package me.jfenn.bingo.common.team

import assertk.assertThat
import assertk.assertions.hasSize
import io.mockk.*
import me.jfenn.bingo.common.data.ScopedData
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.ITeamManager
import me.jfenn.bingo.platform.player.PlayerProfile
import org.junit.jupiter.api.Test
import java.util.*

class TeamServiceTest {

    private fun mockTeamPreset(name: String) =
        BingoTeamKey(name) to BingoTeamPreset(mockk(relaxed = true))

    private fun mockPlayer() =
        mockk<IPlayerHandle>().also {
            val uuid = UUID.randomUUID()
            every { it.uuid } returns uuid
            every { it.profile } returns PlayerProfile(uuid, "")
            every { it.sendMessage(any()) } just Runs
        }

    private val mockScopedData = mockk<ScopedData>().also {
        every { it.teamPresets } returns mapOf(
            mockTeamPreset("blue"),
            mockTeamPreset("yellow"),
            mockTeamPreset("green"),
            mockTeamPreset("orange"),
        )
    }

    private fun fakeBingoState() = mockk<BingoState>().also {
        val teams = mutableMapOf<BingoTeamKey, BingoTeam>()
        every { it.teams } returns teams
        every { it.playersSpectatingIds } returns mutableSetOf()

        val captureTeam = slot<BingoTeam>()
        every { it.registerTeam(capture(captureTeam)) } answers {
            val team = captureTeam.captured
            teams[team.key] = team
            team
        }
    }

    @Test
    fun `Given 7 players, when shuffling into 3 teams, the players should be shuffled correctly`() {
        val state = fakeBingoState()
        val teamManager = mockk<ITeamManager>().also {
            every { it.createTeam(any(), any(), any()) } returns mockk()
            // no players have picked a team yet
            every { it.getPlayerTeam(any()) } returns null
            every { it.setPlayerTeam(any(), any()) } returns Unit
        }
        val playerManager = mockk<IPlayerManager>().also {
            every { it.getPlayers() } returns List(7) { mockPlayer() }
        }
        val teamService = TeamService(
            log = mockk(relaxed = true),
            eventBus = mockk(relaxed = true),
            state = state,
            data = mockScopedData,
            teamManager = teamManager,
            text = mockk(relaxed = true),
            playerManager = playerManager,
            mapService = mockk(),
            permissions = mockk(),
        )

        teamService.shuffleTeams(3)

        val teamEntries = state.teams.values.sortedBy { it.players.size }
        assertThat(teamEntries).hasSize(3)
        assertThat(teamEntries[0].players).hasSize(2)
        assertThat(teamEntries[1].players).hasSize(2)
        assertThat(teamEntries[2].players).hasSize(3)
    }

    @Test
    fun `Given 3 players, when shuffling into 2 teams, the players should be shuffled correctly`() {
        val state = fakeBingoState()
        val teamManager = mockk<ITeamManager>().also {
            every { it.createTeam(any(), any(), any()) } returns mockk()
            // no players have picked a team yet
            every { it.getPlayerTeam(any()) } returns null
            every { it.setPlayerTeam(any(), any()) } returns Unit
        }
        val playerManager = mockk<IPlayerManager>().also {
            every { it.getPlayers() } returns List(3) { mockPlayer() }
        }
        val teamService = TeamService(
            log = mockk(relaxed = true),
            eventBus = mockk(relaxed = true),
            state = state,
            data = mockScopedData,
            teamManager = teamManager,
            text = mockk(relaxed = true),
            playerManager = playerManager,
            mapService = mockk(),
            permissions = mockk(),
        )

        teamService.shuffleTeams(2)

        val teamEntries = state.teams.values.sortedBy { it.players.size }
        assertThat(teamEntries).hasSize(2)
        assertThat(teamEntries[0].players).hasSize(1)
        assertThat(teamEntries[1].players).hasSize(2)
    }

    @Test
    fun `Given 2 players, when shuffling into 3 teams, the players should be shuffled correctly`() {
        val state = fakeBingoState()
        val teamManager = mockk<ITeamManager>().also {
            every { it.createTeam(any(), any(), any()) } returns mockk()
            // no players have picked a team yet
            every { it.getPlayerTeam(any()) } returns null
            every { it.setPlayerTeam(any(), any()) } returns Unit
        }
        val playerManager = mockk<IPlayerManager>().also {
            every { it.getPlayers() } returns List(2) { mockPlayer() }
        }
        val teamService = TeamService(
            log = mockk(relaxed = true),
            eventBus = mockk(relaxed = true),
            state = state,
            data = mockScopedData,
            teamManager = teamManager,
            text = mockk(relaxed = true),
            playerManager = playerManager,
            mapService = mockk(),
            permissions = mockk(),
        )

        teamService.shuffleTeams(3)

        val teamEntries = state.teams.values.sortedBy { it.players.size }
        assertThat(teamEntries).hasSize(2)
        assertThat(teamEntries[0].players).hasSize(1)
        assertThat(teamEntries[1].players).hasSize(1)
    }

    @Test
    fun `Given only 2 presets, when shuffling into 3 teams, a temporary team should be created`() {
        val state = fakeBingoState()
        val teamManager = mockk<ITeamManager>().also {
            every { it.createTeam(any(), any(), any()) } returns mockk()
            // no players have picked a team yet
            every { it.getPlayerTeam(any()) } returns null
            every { it.setPlayerTeam(any(), any()) } returns Unit
        }
        val playerManager = mockk<IPlayerManager>().also {
            every { it.getPlayers() } returns List(3) { mockPlayer() }
        }
        val mockScopedData = mockk<ScopedData>().also {
            every { it.teamPresets } returns mapOf(
                mockTeamPreset("blue"),
                mockTeamPreset("yellow"),
            )
        }
        val teamService = TeamService(
            log = mockk(relaxed = true),
            eventBus = mockk(relaxed = true),
            state = state,
            data = mockScopedData,
            teamManager = teamManager,
            text = mockk(relaxed = true),
            playerManager = playerManager,
            mapService = mockk(),
            permissions = mockk(),
        )

        teamService.shuffleTeams(3)

        val teamEntries = state.teams.values.sortedBy { it.players.size }
        assertThat(teamEntries).hasSize(3)
        assertThat(teamEntries[0].players).hasSize(1)
        assertThat(teamEntries[1].players).hasSize(1)
        assertThat(teamEntries[2].players).hasSize(1)
        assertThat(teamEntries.filter { it.isTemporary }).hasSize(1)
    }
}