---
description: Configure custom BINGO functions to run in your Minecraft datapack.
---

# Function Tags

The mod provides a few events for datapacks to use as [function tags](https://minecraft.wiki/w/Tag#Function_tags).

To create a function that runs on an event, you'll need both a function file and a `tags/.../[event].json` file that references its ID. For example:

::: code-group

```mcfunction [mynamespace/functions/reset_all_my_scoreboards.mcfunction]
scoreboard players reset * [...]
```

:::

::: code-group

```json [yet-another-minecraft-bingo/tags/functions/on_playing.json]
{
  "values": [
    "mynamespace:reset_all_my_scoreboards"
  ]
}
```

:::


::: info
While the function can be wherever you want (replacing `mynamespace` with something specific to your datapack), it's important that the
JSON tags file is under the `yet-another-minecraft-bingo` namespace. Otherwise, the mod won't be able to find it!
:::

## `#yet-another-minecraft-bingo:on_pregame`

This runs as soon as the server starts up, if a game has not been started.

## `#yet-another-minecraft-bingo:on_loading`

This runs when the "Start!" button is first pressed, while the game is attempting to locate a spawnpoint and teleport players.

## `#yet-another-minecraft-bingo:on_countdown`

This runs once the game has teleported players to their spawnpoints, just before the countdown starts.

## `#yet-another-minecraft-bingo:on_playing`

This runs at the end of the countdown, when the game is started.

::: tip
It is recommended to use `on_playing` to reset any scoreboards or advancements that your datapack includes.
:::

## `#yet-another-minecraft-bingo:on_postgame`

This runs when the game is over.
