### New Features:

- Added an "SMP Mode" for dedicated servers that runs BINGO games on the same world without erasing world data

  This is now the default behavior when the mod is installed on a server. See [the wiki](https://minecraft.horrific.dev/bingo/server-setup) for more information!
  (including how to switch to Lobby Mode)

- Various improvements to the game over screen, including some fancy animations!

- Scored items, leading teams, and other game messages are now drawn as part of the HUD instead of spamming the chat log!

- The list of players on a team is now shown in a tooltip when hovering over the card title.

- Multiple tier lists can be selected at once - e.g. for a card with both items and advancements.

  These are weighted evenly by default (meaning an even portion of the item distribution goes to each tier list, after rounding) - but can be adjusted with commands!

- Datapack/scoreboard data is now preserved between rounds by default (on dedicated servers only)

- Added [function files](https://minecraft.horrific.dev/bingo/functions) (under `config/yet-another-minecraft-bingo/commands/on_countdown.mcfunction`) that can be configured to run commands on certain game events.

### Bugs Squashed:

- Using lockout mode with a "full card" goal no longer ends the game due to a stalemate.

- Fixed an obscure crash caused when a tier list sends invalid items to the client.

- Fixed incompatibility with servers using a different `level-name` value in `server.properties`.

- The server no longer overwrites certain gamerules if they are set before a round is started.
