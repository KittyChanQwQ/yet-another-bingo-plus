---
description: Configure custom BINGO functions to run on a Minecraft server.
---

# Functions

Several `.mcfunction` files are available in the mod's config folder. These can be used to run commands on certain game events, such as when a round starts or ends.

```txt{5-9}
🗀 config/
 └ 🗀 yet-another-minecraft-bingo/
   ├ config.json
   └ commands/
     ├ on_pregame.mcfunction
     ├ on_loading.mcfunction
     ├ on_countdown.mcfunction
     ├ on_playing.mcfunction
     └ on_postgame.mcfunction
🗀 mods/
🗀 world/
server.properties
```

For example, `on_playing` can be used to set certain gamerules that you might want during a round - e.g. `gamerule announceAdvancements false`.

These events are also available to datapacks in the form of [function tags](https://minecraft.wiki/w/Tag#Function_tags).

## `#yet-another-minecraft-bingo:on_pregame`

This runs as soon as the server starts up, if a game has not been started.

## `#yet-another-minecraft-bingo:on_loading`

This runs when the "Start!" button is first pressed, while the game is attempting to locate a spawnpoint and teleport players.

## `#yet-another-minecraft-bingo:on_countdown`

This runs once the game has teleported players to their spawnpoints, just before the countdown starts.

## `#yet-another-minecraft-bingo:on_playing`

This runs at the end of the countdown, when the game is started.

## `#yet-another-minecraft-bingo:on_postgame`

This runs when the game is over.
