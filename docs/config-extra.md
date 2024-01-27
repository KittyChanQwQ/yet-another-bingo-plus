# Additional config options

```txt{3}
🗀 config/
 └ 🗀 bingo/
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

## `"autoShutDownInPostgame"`
(default: true)

Automatically shuts down / restarts the server if all players disconnect after a game has ended.

## `"unsafeSkipWorldClose"`
(default: false)

Skips world closing when the server is shutting down, which can speed up restarts if the server is held up by chunk saving. This may cause crashes or world data corruption.
