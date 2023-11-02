package me.jfenn.bingo.config

import kotlinx.serialization.Serializable

@Serializable
data class BingoConfig(
    var overwrite: Boolean = true,
    var gameMode: BingoMode = BingoMode.STANDARD,
    var winCondition: WinCondition = WinCondition.THREE_LINES,
    var timerMinutes: Int? = 60,
    var itemDistribution: List<Int> = listOf(2, 6, 9, 6, 2),
    var isElytra: Boolean = false,
    var isUnlockRecipes: Boolean = true,
    var isPvpEnabled: Boolean = true,
    var isKeepInventory: Boolean = false,
    var spreadPlayersDistance: Int = 100,


    var refillSpawnEquipmentOnRespawn: Boolean = true,
    var isSpawnEquipment: Boolean = true,
    var spawnEquipmentItems: List<SpawnItem> = listOf(
        SpawnItem("minecraft:iron_chestplate"),
        SpawnItem("minecraft:iron_pickaxe"),
        SpawnItem("minecraft:iron_axe"),
        SpawnItem("minecraft:shield"),
        SpawnItem("minecraft:torch", count = 64),
    ),

    // TODO: create a spawn chest per team
    // var isSpawnChest: Boolean = true,
    // var spawnChestItems: List<SpawnItem> = listOf(
    //    SpawnItem("minecraft:iron_ingot", count = 64),
    //    SpawnItem("minecraft:oak_log", count = 64),
    //    SpawnItem("minecraft:torch", count = 64),
    // ),

    var unsafeSkipWorldClose: Boolean = false,
) {

    fun isValid(): Boolean {
        return itemDistribution.sum() == 25
    }

    @Serializable
    data class SpawnItem(
        val id: String,
        val nbt: String = "{}",
        val count: Int = 1,
    )

}