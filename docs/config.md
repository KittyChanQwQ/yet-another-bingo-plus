# Configuration

The BINGO mod creates several config files the first time you start it up:

```txt{3-5}
🗀 config/
 └ 🗀 bingo/
   ├ ... _tier.txt
   ├ config.json
   └ players.json
🗀 mods/
🗀 world/
server.properties
```

- `*_tier.txt` files are used to generate the BINGO card from a set of [tier lists](./tier-editor).
- `config.json` includes the current game configuration and some additional settings.
- `players.json` contains player-specific settings, such as whether to display info in a bossbar or scoreboard.

Most config properties can be edited in-game, using the menu in the pre-game lobby.

## Additional config options

However, some of these properties are only editable in the file:

- `"overwrite"` (default: true)

  Specifies whether changes to the game configuration should persist in the file. If true, changing in-game settings (win condition, item distribution, time limit) will update those values.

- `"spreadPlayersDistance"` (default: 100)

  Determines the minimum blocks apart that team spawns should be placed from each other. (which is usually much greater than whatever value it's set to)

  This is approximated by placing each team in a circle with a radius of $distance \times count \times \frac{1}{2\pi}$ from the origin, and then finding an appropriate spawn outwards from that point.

- `"excludeSpawnKitItemsFromCards"` (default: true)

  Prevents cards from generating with any of the items that are in the enabled spawn kits.

- `"preventScoringSpawnKitItems"` (default: false)

  Adds NBT tags to all spawn kit items, which prevents them from being scored. However, this also [prevents them from being auto-filled in crafting recipes](https://gitlab.com/horrific-tweaks/bingo/-/issues/16) due to Minecraft client behavior ([MC-129057](https://bugs.mojang.com/browse/MC-129057)), hence why this is turned off by default.

- `"refillPlayerKitOnRespawn"` (default: true)

  Specifies whether players should get another player spawn kit when they respawn. If false, players will only get one spawn kit at the start of the game.

- `"autoShutDownInPostgame"` (default: true)

  Automatically shuts down / restarts the server if all players disconnect after a game has ended.

- `"unsafeSkipWorldClose"` (default: false)

  Skips world closing when the server is shutting down, which can speed up restarts if the server is held up by chunk saving. This may cause crashes or world data corruption.
