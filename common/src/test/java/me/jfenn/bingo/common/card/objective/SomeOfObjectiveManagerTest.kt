package me.jfenn.bingo.common.card.objective

import assertk.assertThat
import assertk.assertions.containsOnly
import io.mockk.every
import io.mockk.mockk
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.BingoCardEntry
import me.jfenn.bingo.common.card.data.ObjectiveData
import me.jfenn.bingo.common.card.data.ObjectiveDataReference
import me.jfenn.bingo.common.options.BingoCardOptions
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.team.OfflinePlayerCache
import me.jfenn.bingo.common.utils.seconds
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.player.PlayerProfile
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class SomeOfObjectiveManagerTest {

    private val t0: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val red = PlayerProfile(UUID.randomUUID(), "red")
    private val blue = PlayerProfile(UUID.randomUUID(), "blue")

    @Test
    fun `lockout all_of awards the team that completed the set first`() {
        // red holds 1/4 at t=5, completes the rest at t=20..22
        // blue completes 4/4 between t=11..14 (legitimate winner)
        val items = listOf(
            item("a", red to 5, blue to 11),
            item("b", red to 20, blue to 12),
            item("c", red to 21, blue to 13),
            item("d", red to 22, blue to 14),
        )
        val root = allOf("all", items)

        tick(card(root, items))

        assertThat(root.teamsAchieved.keys).containsOnly(BingoTeamKey("blue"))
    }

    @Test
    fun `lockout one_of awards the team with the earliest sub-objective`() {
        val items = listOf(
            item("x", red to 4),
            item("y", blue to 8),
            item("z", blue to 9),
        )
        val root = oneOf("one", items)

        tick(card(root, items))

        assertThat(root.teamsAchieved.keys).containsOnly(BingoTeamKey("red"))
    }

    @Test
    fun `lockout some_of awards the team that hit valueMin first`() {
        // valueMin=2; red qualifies at t=20 (2nd capture), blue at t=11 (2nd capture)
        val items = listOf(
            item("p", red to 2, blue to 10),
            item("q", red to 20, blue to 11),
            item("r", blue to 12),
            item("s"),
        )
        val root = someOf("some", items, valueMin = 2)

        tick(card(root, items))

        assertThat(root.teamsAchieved.keys).containsOnly(BingoTeamKey("blue"))
    }

    private fun item(id: String, vararg captures: Pair<PlayerProfile, Int>) =
        BingoObjective.ItemEntry(id = id, data = ObjectiveData.Item(item = id))
            .apply {
                captures.forEach { (p, t) ->
                    playersHolding[p.uuid] = BingoObjectiveCapture(
                        team = BingoTeamKey(p.name),
                        player = p,
                        instant = t0 + t.seconds,
                    )
                }
            }

    private fun refs(items: List<BingoObjective.ItemEntry>) =
        items.map { ObjectiveDataReference.Id(it.id) }.toSet()

    private fun allOf(id: String, items: List<BingoObjective.ItemEntry>) = BingoObjective.SomeOfEntry(
        id = id,
        data = ObjectiveData.AllOf(objectives = refs(items)),
        someOfObjectives = items.map { it.id }.toSet(),
        valueMin = items.size,
        valueMax = items.size,
    )

    private fun oneOf(id: String, items: List<BingoObjective.ItemEntry>) = BingoObjective.SomeOfEntry(
        id = id,
        data = ObjectiveData.OneOf(objectives = refs(items)),
        someOfObjectives = items.map { it.id }.toSet(),
        valueMin = 1,
        valueMax = items.size,
    )

    private fun someOf(id: String, items: List<BingoObjective.ItemEntry>, valueMin: Int) = BingoObjective.SomeOfEntry(
        id = id,
        data = ObjectiveData.SomeOf(objectives = refs(items)),
        someOfObjectives = items.map { it.id }.toSet(),
        valueMin = valueMin,
        valueMax = items.size,
    )

    private fun card(root: BingoObjective.SomeOfEntry, items: List<BingoObjective.ItemEntry>): BingoCard {
        val freeSpace = BingoObjective.FreeSpace()
        return BingoCard(
            id = CARD_ID,
            entries = listOf(BingoCardEntry(root.id, null, null)) +
                    List(24) { BingoCardEntry(freeSpace.id, null, null) },
            objectives = mapOf(root.id to root) + items.associateBy { it.id } + (freeSpace.id to freeSpace),
            options = BingoCardOptions(isLockoutMode = true),
        )
    }

    private fun tick(card: BingoCard) {
        val redTeam = mockTeam(red)
        val blueTeam = mockTeam(blue)
        val state = mockk<BingoState>(relaxed = true).also {
            every { it.getRegisteredTeams() } returns listOf(redTeam, blueTeam)
            every { it.teams } returns mutableMapOf(redTeam.key to redTeam, blueTeam.key to blueTeam)
            every { it.updatedAt } returns t0 + 60.seconds
        }
        val cache = mockk<OfflinePlayerCache>().also {
            every { it.getOfflinePlayer(red) } returns mockHandle(red)
            every { it.getOfflinePlayer(blue) } returns mockHandle(blue)
        }
        SomeOfObjectiveManager(
            log = LoggerFactory.getLogger(this::class.java),
            data = mockk(relaxed = true),
            objectiveDisplayService = mockk(relaxed = true),
            objectiveService = ObjectiveService(state, cache),
            tagExpansionService = mockk(relaxed = true),
            text = mockk(relaxed = true),
        ).tick(card)
    }

    private fun mockTeam(player: PlayerProfile) = mockk<BingoTeam>(relaxed = true).also {
        every { it.id } returns player.name
        every { it.key } returns BingoTeamKey(player.name)
        every { it.players } returns mutableSetOf(player)
        every { it.cardId } returns CARD_ID
        every { it.isPlaying() } returns true
        every { it.winner } returns null
    }

    private fun mockHandle(profile: PlayerProfile) = mockk<IPlayerHandle>(relaxed = true).also {
        every { it.uuid } returns profile.uuid
        every { it.profile } returns profile
    }

    private companion object {
        val CARD_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    }
}
