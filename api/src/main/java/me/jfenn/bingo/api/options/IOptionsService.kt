package me.jfenn.bingo.api.options

import java.time.Duration

interface IOptionsService {
    var winCondition: BingoWinCondition
    var stalemateBehavior: StalemateBehavior
    var endGameWhen: EndWhen
    var timeLimit: Duration?
    var isElytra: Boolean
    var isNightVision: Boolean
    var isUnlockRecipes: Boolean
    var isPvpEnabled: Boolean
    var isKeepInventory: Boolean
    var spawnDimension: String
    var spawnDistance: Int
    var showPreviewCard: Boolean
    var isPlayerKit: Boolean
    var isTeamKit: Boolean
}