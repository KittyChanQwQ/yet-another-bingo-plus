@file:Suppress("unused", "FunctionName")

package me.jfenn.bingo.common.stats

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.mockk.mockk
import kotlinx.datetime.Instant
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.options.BingoOptions
import me.jfenn.bingo.common.stats.data.GameInfo
import me.jfenn.bingo.common.stats.data.GamePlayerInfo
import me.jfenn.bingo.common.stats.data.GameTeamInfo
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.test.BaseGameTest
import me.jfenn.bingo.common.utils.EventListener
import me.jfenn.bingo.platform.ICloseEvent
import me.jfenn.bingo.sql.BingoDatabase
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.test.GameTest
import net.minecraft.test.TestContext
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class StatsServiceTest : BaseGameTest() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val closeEvent = object : ICloseEvent {
        override val onClose: EventListener<Unit> = EventListener()
    }

    private val connectionFactory = ConnectionFactory(
        log = logger,
        config = BingoConfig(
            databaseUrl = "jdbc:sqlite:file:memdb1?mode=memory"
        ),
        closeEvent = closeEvent,
    )

    private fun createStatsService(
        db: BingoDatabase,
        hostId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    ) = StatsService(logger, db, mockk(relaxed = true), BingoConfig(statsHostId = hostId))

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun startsAndRunsMigrations(context: TestContext) {
        connectionFactory.create()
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun checkIfIdsExist_AlreadyExists(context: TestContext) {
        val db = connectionFactory.create()
        val service = createStatsService(db)

        val playerId = UUID.fromString("c55e11bd-11a5-47ca-b0f6-aa38b42ed434")
        val gameId = UUID.fromString("8d94af8b-bfb9-452c-bc8b-945f183c5797")
        val hostId = UUID.fromString("30d5d5ea-c63e-4c48-983c-04a25457bdf2")

        // Insert gameId with a record for playerId
        service.insertGame(
            GameInfo(
                id = gameId,
                bingoOptions = "options1",
                bingoOptionsHash = "options1",
                startedAt = Instant.parse("2002-02-02T02:02:02Z"),
                endedAt = Instant.parse("2002-02-02T02:02:02Z"),
                duration = 2.minutes,
                playerCount = 1,
                isDraw = false,
                isForfeit = false,
                hostId = hostId,
            ),
            listOf(
                GameTeamInfo(
                    id = BingoTeamKey("bingo_blue"),
                    gameId = gameId,
                    name = "bingo.team.blue",
                    isWinner = true,
                )
            ),
            listOf(
                GamePlayerInfo(
                    teamId = BingoTeamKey("bingo_blue"),
                    gameId = gameId,
                    minecraftName = "Player1",
                    minecraftId = playerId,
                    capturedItems = 25,
                )
            )
        )

        val checkIds = service.checkIfIdsExist(playerId, setOf(gameId))
        assertThat(checkIds).isEqualTo(emptySet())
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun checkIfIdsExist_MissingGameId(context: TestContext) {
        val db = connectionFactory.create()
        val service = createStatsService(db)

        val playerId = UUID.fromString("c55e11bd-11a5-47ca-b0f6-aa38b42ed434")

        // Create a gameId that doesn't exist anywhere in the db
        val gameIdMissing = UUID.fromString("7deb418a-edf9-4177-ab3f-2bf376be00bd")

        // The searched gameId should be returned because it doesn't exist
        val checkIds = service.checkIfIdsExist(playerId, setOf(gameIdMissing))
        assertThat(checkIds).isEqualTo(setOf(gameIdMissing))
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun checkIfIdsExist_MissingPlayerId(context: TestContext) {
        val db = connectionFactory.create()
        val service = createStatsService(db)

        val playerId = UUID.fromString("c55e11bd-11a5-47ca-b0f6-aa38b42ed434")
        val gameId = UUID.fromString("8d94af8b-bfb9-452c-bc8b-945f183c5797")
        val hostId = UUID.fromString("30d5d5ea-c63e-4c48-983c-04a25457bdf2")

        // Insert gameId that does not have a record for the playerId
        service.insertGame(
            GameInfo(
                id = gameId,
                bingoOptions = "options1",
                bingoOptionsHash = "options1",
                startedAt = Instant.parse("2002-02-02T02:02:02Z"),
                endedAt = Instant.parse("2002-02-02T02:02:02Z"),
                duration = 2.minutes,
                playerCount = 1,
                isDraw = false,
                isForfeit = false,
                hostId = hostId,
            ),
            listOf(
                GameTeamInfo(
                    id = BingoTeamKey("bingo_blue"),
                    gameId = gameId,
                    name = "bingo.team.blue",
                    isWinner = true,
                )
            ),
            listOf(
                GamePlayerInfo(
                    teamId = BingoTeamKey("bingo_blue"),
                    gameId = gameId,
                    minecraftName = "Player1",
                    // This is not the same playerId!
                    minecraftId = UUID.fromString("b1be9b57-9ccd-4ec6-9272-4316c70ca672"),
                    capturedItems = 25,
                )
            )
        )

        // The searched gameId should be returned becuase it doesn't have an entry for the player record
        val checkIds = service.checkIfIdsExist(playerId, setOf(gameId))
        assertThat(checkIds).isEqualTo(setOf(gameId))
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun getPlayerSummary_Nonexistent(context: TestContext) {
        val db = connectionFactory.create()
        val service = createStatsService(db)

        val summary = service.getPlayerSummary(UUID.fromString("41834902-040e-467b-82a7-bdaad063b718"))
        assertThat(summary.totalPlaytime).isEqualTo(Duration.ZERO)
        assertThat(summary.totalItems).isEqualTo(0L)
        assertThat(summary.totalWins).isEqualTo(0L)
        assertThat(summary.totalLosses).isEqualTo(0L)
        assertThat(summary.totalGames).isEqualTo(0L)
        assertThat(summary.favoriteTeam).isNull()
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun getPlayerSummary_Monthly(context: TestContext) {
        val db = connectionFactory.create()
        val service = createStatsService(db)
        val playerId = UUID.fromString("80f963e1-213e-412c-9d89-bb2986bafd0c")
        val hostId = UUID.fromString("30d5d5ea-c63e-4c48-983c-04a25457bdf2")

        // Insert a won game in 2002-02-02
        val wonGameId = UUID.fromString("78dae7ea-22ad-41b5-8ba7-54eb9e033250")
        val feb02 = Instant.parse("2002-02-02T02:02:02Z")
        service.insertGame(
            GameInfo(
                id = wonGameId,
                bingoOptions = "options1",
                bingoOptionsHash = "options1",
                startedAt = feb02,
                endedAt = feb02 + 2.minutes,
                duration = 2.minutes,
                playerCount = 1,
                isDraw = false,
                isForfeit = false,
                hostId = hostId,
            ),
            listOf(
                GameTeamInfo(
                    id = BingoTeamKey("bingo_blue"),
                    gameId = wonGameId,
                    name = "bingo.team.blue",
                    isWinner = true,
                )
            ),
            listOf(
                GamePlayerInfo(
                    teamId = BingoTeamKey("bingo_blue"),
                    gameId = wonGameId,
                    minecraftName = "Player1",
                    minecraftId = playerId,
                    capturedItems = 25,
                )
            )
        )

        // Insert a lost game in 2002-03-03
        val lostGameId = UUID.fromString("02f05fe4-a12f-4add-bb34-d4ad943059e6")
        val mar03 = Instant.parse("2002-03-03T02:02:02Z")
        service.insertGame(
            GameInfo(
                id = lostGameId,
                bingoOptions = "options1",
                bingoOptionsHash = "options1",
                startedAt = mar03,
                endedAt = mar03 + 2.minutes,
                duration = 2.minutes,
                playerCount = 1,
                isDraw = false,
                isForfeit = false,
                hostId = hostId,
            ),
            listOf(
                GameTeamInfo(
                    id = BingoTeamKey("bingo_blue"),
                    gameId = lostGameId,
                    name = "bingo.team.blue",
                    isWinner = false,
                )
            ),
            listOf(
                GamePlayerInfo(
                    teamId = BingoTeamKey("bingo_blue"),
                    gameId = lostGameId,
                    minecraftName = "Player1",
                    minecraftId = playerId,
                    capturedItems = 24,
                )
            )
        )

        // Query the summary on 2002-03-23
        val mar23 = Instant.parse("2002-03-23T02:02:02Z")
        val summary = service.getPlayerSummary(playerId, now = mar23)

        // - The total summary should include both games
        assertThat(summary.totalPlaytime).isEqualTo(4.minutes)
        assertThat(summary.totalItems).isEqualTo(49)
        assertThat(summary.totalWins).isEqualTo(1)
        assertThat(summary.totalLosses).isEqualTo(1)
        assertThat(summary.totalGames).isEqualTo(2)

        // - The monthly summary should only include the second game
        assertThat(summary.monthlyPlaytime).isEqualTo(2.minutes)
        assertThat(summary.monthlyItems).isEqualTo(24)
        assertThat(summary.monthlyWins).isEqualTo(0)
        assertThat(summary.monthlyLosses).isEqualTo(1)
        assertThat(summary.monthlyGames).isEqualTo(1)

        assertThat(summary.favoriteTeam).isEqualTo(BingoTeamKey("bingo_blue"))
        assertThat(summary.favoriteTeamPercentage).isEqualTo(1f)
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun updateBestStats_DecreasesBestTime(context: TestContext) {
        val db = connectionFactory.create()
        val hostId = UUID.fromString("608714c6-8520-4ab1-aba7-20ad5a3f6da7")
        val service = createStatsService(db, hostId = hostId)

        val options = BingoOptions()

        // Insert a game with duration=3 mins
        val gameOneId = UUID.fromString("02f05fe4-a12f-4add-bb34-d4ad943059e6")
        service.updateBestStats(
            GameInfo(
                id = gameOneId,
                bingoOptions = "options1",
                bingoOptionsHash = options.getShaHash(),
                startedAt = Instant.DISTANT_PAST,
                endedAt = Instant.DISTANT_PAST,
                duration = 3.minutes,
                playerCount = 1,
                isDraw = false,
                isForfeit = false,
                hostId = hostId,
            ),
            emptyList(),
        )

        // The "best time" should be 3 minutes
        assertThat(
            service.getBestTime(options, false)
        ).isEqualTo(3.minutes)

        // Insert a game with duration=2 mins
        val gameTwoId = UUID.fromString("a3301200-95be-4207-a784-34beb02f6644")
        service.updateBestStats(
            GameInfo(
                id = gameTwoId,
                bingoOptions = "options1",
                bingoOptionsHash = options.getShaHash(),
                startedAt = Instant.DISTANT_PAST,
                endedAt = Instant.DISTANT_PAST,
                duration = 2.minutes,
                playerCount = 1,
                isDraw = false,
                isForfeit = false,
                hostId = hostId,
            ),
            emptyList(),
        )

        // The "best time" should now be 2 minutes
        assertThat(
            service.getBestTime(options, false)
        ).isEqualTo(2.minutes)
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun updateBestStats_DoesNotMixSingleplayer(context: TestContext) {
        val db = connectionFactory.create()
        val hostId = UUID.fromString("664fb41c-ec4c-4f9a-b8e0-2acb41accc29")
        val service = createStatsService(db, hostId = hostId)

        val options = BingoOptions()

        // Insert a singleplayer game with duration=3 mins
        val gameOneId = UUID.fromString("02f05fe4-a12f-4add-bb34-d4ad943059e6")
        val gameOne = GameInfo(
            id = gameOneId,
            bingoOptions = "options1",
            bingoOptionsHash = options.getShaHash(),
            startedAt = Instant.DISTANT_PAST,
            endedAt = Instant.DISTANT_PAST,
            duration = 3.minutes,
            playerCount = 1,
            isDraw = false,
            isForfeit = false,
            hostId = hostId,
        )
        service.insertGame(gameOne, emptyList(), emptyList())
        service.updateBestStats(gameOne, emptyList())

        // The "best time" should be 3 minutes
        assertThat(
            service.getBestTime(options, false)
        ).isEqualTo(3.minutes)
        assertThat(
            service.getBestTime(options, true)
        ).isEqualTo(3.minutes)

        // Insert a MULTIPLAYER game with duration=2 mins
        val gameTwoId = UUID.fromString("a3301200-95be-4207-a784-34beb02f6644")
        val gameTwo = GameInfo(
            id = gameTwoId,
            bingoOptions = "options1",
            bingoOptionsHash = options.getShaHash(),
            startedAt = Instant.DISTANT_PAST,
            endedAt = Instant.DISTANT_PAST,
            duration = 2.minutes,
            playerCount = 5,
            isDraw = false,
            isForfeit = false,
            hostId = hostId,
        )
        service.insertGame(gameTwo, emptyList(), emptyList())
        service.updateBestStats(gameTwo, emptyList())

        // The multiplayer "best time" should be 2 minutes
        assertThat(
            service.getBestTime(options, false)
        ).isEqualTo(2.minutes)
        // However, the singleplayer best time should still be 3 minutes
        assertThat(
            service.getBestTime(options, true)
        ).isEqualTo(3.minutes)
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun updateBestStats_DoesNotMixHosts(context: TestContext) {
        val db = connectionFactory.create()
        // Create two different stats services, with different hostIds
        val hostId = UUID.fromString("608714c6-8520-4ab1-aba7-20ad5a3f6da7")
        val hostId2 = UUID.fromString("1f18ec8e-3c5d-4754-9802-3538706b11c0")
        val service = createStatsService(db, hostId = hostId)
        val service2 = createStatsService(db, hostId = hostId2)

        val options = BingoOptions()

        // Insert a game with duration=3 mins
        val gameOneId = UUID.fromString("02f05fe4-a12f-4add-bb34-d4ad943059e6")
        service.updateBestStats(
            GameInfo(
                id = gameOneId,
                bingoOptions = "options1",
                bingoOptionsHash = options.getShaHash(),
                startedAt = Instant.DISTANT_PAST,
                endedAt = Instant.DISTANT_PAST,
                duration = 3.minutes,
                playerCount = 1,
                isDraw = false,
                isForfeit = false,
                hostId = hostId,
            ),
            emptyList(),
        )

        // The "best time" should be 3 minutes
        assertThat(
            service.getBestTime(options, false)
        ).isEqualTo(3.minutes)

        // Insert a game with duration=2 mins, but from a different hostId
        val gameTwoId = UUID.fromString("a3301200-95be-4207-a784-34beb02f6644")
        service2.updateBestStats(
            GameInfo(
                id = gameTwoId,
                bingoOptions = "options1",
                bingoOptionsHash = options.getShaHash(),
                startedAt = Instant.DISTANT_PAST,
                endedAt = Instant.DISTANT_PAST,
                duration = 2.minutes,
                playerCount = 1,
                isDraw = false,
                isForfeit = false,
                hostId = hostId2,
            ),
            emptyList(),
        )

        // The "best time" should still be 3 minutes
        assertThat(
            service.getBestTime(options, false)
        ).isEqualTo(3.minutes)

        // The "best time" from service2 should be 2 minutes
        assertThat(
            service2.getBestTime(options, false)
        ).isEqualTo(2.minutes)
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    fun updateBestStats_DoesNotIncreaseBestTime(context: TestContext) {
        val db = connectionFactory.create()
        val hostId = UUID.fromString("01c44a53-1fd8-49c0-9709-fdc1a657c4e9")
        val service = createStatsService(db, hostId = hostId)

        val options = BingoOptions()

        // Insert a game with duration=3 mins
        val gameOneId = UUID.fromString("02f05fe4-a12f-4add-bb34-d4ad943059e6")
        service.updateBestStats(
            GameInfo(
                id = gameOneId,
                bingoOptions = "options1",
                bingoOptionsHash = options.getShaHash(),
                startedAt = Instant.DISTANT_PAST,
                endedAt = Instant.DISTANT_PAST,
                duration = 3.minutes,
                playerCount = 1,
                isDraw = false,
                isForfeit = false,
                hostId = hostId,
            ),
            emptyList(),
        )

        // The "best time" should be 3 minutes
        assertThat(
            service.getBestTime(options, false)
        ).isEqualTo(3.minutes)

        // Insert a game with duration=4 mins
        val gameTwoId = UUID.fromString("a3301200-95be-4207-a784-34beb02f6644")
        service.updateBestStats(
            GameInfo(
                id = gameTwoId,
                bingoOptions = "options1",
                bingoOptionsHash = options.getShaHash(),
                startedAt = Instant.DISTANT_PAST,
                endedAt = Instant.DISTANT_PAST,
                duration = 4.minutes,
                playerCount = 1,
                isDraw = false,
                isForfeit = false,
                hostId = hostId,
            ),
            emptyList(),
        )

        // The "best time" should still be 3 minutes
        assertThat(
            service.getBestTime(options, false)
        ).isEqualTo(3.minutes)
    }

    override fun afterEach() {
        closeEvent.onClose(Unit)
    }

}