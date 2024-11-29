---
description: A list of the customizable behavior in Yet Another Minecraft BINGO's additional config.json options.
---

# Additional config options

```txt{3}
🗀 config/
 └ 🗀 yet-another-minecraft-bingo/
   ├ config.json
   ├ game-options.json
   └ player-settings.json
🗀 mods/
🗀 world/
server.properties
```

These config options can only be changed by manually editing the `config.json` file, and do not have an interface in the pre-game lobby.

## `"excludeSpawnKitItemsFromCards"`
(default: true)

Prevents cards from generating with any of the items that are in the enabled spawn kits.

## `"preventScoringSpawnKitItems"`
(default: false)

Adds NBT tags to all spawn kit items, which prevents them from being scored. However, this also [prevents them from being auto-filled in crafting recipes](https://gitlab.com/horrific-tweaks/bingo/-/issues/16) due to Minecraft client behavior ([MC-129057](https://bugs.mojang.com/browse/MC-129057)), hence why this is turned off by default.

## `"preventLobbyChaos"`
(default: false)

Turns off sounds that can be caused by other players in the pre-game lobby. This prevents large groups from causing a racket, and is a nice option for streamers to avoid distractions.

## `"lobbyTutorialBook"`
(default: true)

Gives players a book that explains how to play the game and adjust player settings when in the lobby.

## `"nightVisionInSpectator"`
(default: true)

Controls whether all spectating players are given the night vision effect.

## `"nightVisionInPostgame"`
(default: true)

Controls whether all players are given the night vision effect when the game is over.

## `"startWhenReadySeconds"`
(default: 600)

The timer that counts down to start the game once all players are on a team. Players can vote `/ready` to speed this up, proportionally to the player count. Can be set to `null` to turn off.

## `"nextRoundWhenReadySeconds"`
(default: 600)

The timer that counts down to restart/reset the server when a game is over. Can be set to `null` to turn off.

## `"nextRoundWhenReadyWaitsForFirstVote"`
(default: true)

Whether the "Next Round" timer should wait until at least one player has voted `/ready` to start counting down.

## `"nextRoundWhenEveryoneDisconnects"`
(default: true)

Automatically resets the server if all players disconnect after a game has ended.

## `"nextRoundCompatibilityMode"`
(default: false)

If `true`, the server will perform a full restart after every game, rather than deleting the world while the server is running. This may resolve some rare issues with other mods that don't behave nicely.

## `"unsafeSkipWorldClose"`
(default: false)

Skips world closing when the server is shutting down, which can speed up restarts if the server is held up by chunk saving. This may cause crashes or world data corruption.

## `"integrations"`

### `"vanish"`
(default: true)

Controls whether the mod's [Vanish integration](./mods/vanish) is enabled. If this is turned off, the game will not put spectating players into vanish mode, and will act like the mod isn't installed - you'll need to use its commands manually instead.

### `"chunky"`
(default: true)

Controls whether the mod's [Chunky integration](./mods/chunky) is enabled. If this is turned off, the game will not automatically start/cancel any pre-generation tasks. You'll need to manage these manually instead.

### `"chunkyWorlds"`
(default: `"minecraft:overworld": 8000, "minecraft:the_nether": 1000`)

Specifies the dimensions and distance (in blocks) that Chunky should automatically pre-generate, if the `"chunky"` config is `true`.

### `"voice"`

Provides settings for the mod's [Voice Chat Integration](./mods/voice). If enabled, the mod will create groups for each team at the start of the round, and move players into one combined group when it ends.

## `"databaseUrl"` / `"databaseUser"` / `"databasePass"`
(default: null)

Provides a [JDBC database URL](https://docs.oracle.com/javase/tutorial/jdbc/basics/connecting.html), username, and password. Currently, only SQLite is supported. By default, this uses a `stats.db` file in the mod's config directory.

## `"syncStats"`
(default: true)

Whether to sync player statistics (playtime, win/loss, items scored) with any connected clients. Turning this off will only update the statistics for games that are played on the server.

## `"server"`

### `"isLobbyMode"`
(default: false)

Controls whether ["Lobby Mode"](./server-setup) is enabled.

### `"filesToReset"`

A list of files to delete when the server restarts, provided that `"isLobbyMode"` is true. Entries can use `*` wildcards to refer to multiple files - e.g. `DIM*` matches every file or directory that starts with "DIM".