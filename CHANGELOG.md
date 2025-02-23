### New Features:

- **Added a "Replace Goals" mode!** This option (under Goal > More > Play to...) will replace any lines/items on the card with new items as soon as they are scored.

  (credit to AverageFlame for the suggestion!)

- Added a score message for when items are lost in "Inventory" mode, or when a "Never get" objective is failed

- Added message templates to `config/yet-another-minecraft-bingo/messages/game_start.json` to allow custom start/end/scoreboard text!

- Performance improvements to card update packets when many teams/players are on the server.

- Moved around a few config files to improve update migrations:
  * `/stats.db` is renamed to `/bingo.db`
  * `player-settings.json` is now stored in SQLite

- Added initial [Placeholders API](https://placeholders.pb4.eu/) support! This only includes two filters for now: `%yet-another-minecraft-bingo:time%` and `%yet-another-minecraft-bingo:time_left%` (to be expanded in future updates).

- [Lobby worlds](https://horrific.dev/bingo/lobby-structure/) now support editing the datapack files for custom dimension/biome settings

- The preset lobby teams are also now [customizable via datapacks](https://gitlab.com/horrificdev/bingo/-/tree/main/common/src/main/resources/data/yet-another-minecraft-bingo/yet-another-minecraft-bingo/teams?ref_type=heads)!

- Completely removed the "Disconnect" button from the game over screen (was only used for backwards compatibility)

- Removed the confusing "Skip Card when Lost" setting, which is now inferred from the "End When" menu

- (Client) Tiles with multiple items now display each item as part of the hover tooltip.

- (Client) 'Game over' screen now remembers which tab it was on, and only renders the win firework the first time it's opened.

- (Client) Added an animation for card shuffle/reroll

- (Client) Slight changes to the "Ready up" HUD/keybind behavior

### Bugs Squashed:

- Fixed the card occasionally generating with blank spaces when certain filters are applied

- Fixed players being respawned when relogging if LAN or force-gamemode is used

- Prevent HUD desync with [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) when relogging

- Fixed occasional "{team} completed their card!" messages when starting a new game

- Fixed the lobby menu entities being affected by night vision/fullbright ([#145](https://gitlab.com/horrificdev/bingo/-/issues/145))

- Prevent failures when generating/rerolling the card with certain game settings

- Fixed compatibility with new versions of [Just Enough Items](https://modrinth.com/mod/jei)

- Fixed tooltip description text wrapping translation support

- Fixed compatibility with new versions of Quilt loader

- Fixed some behavior not working correctly in survival/SMP mode

- Fixed the `nextRoundWhenEveryoneDisconnects` setting just not working at all

- The lobby menu no longer breaks in custom lobbies when north-facing signs are used

- Fixed [Waystones](https://modrinth.com/mod/waystones) keeping previous teleport locations after the world is reset

### Translations

- New Chinese Simplified translations by BoredYukolin, teaSummer, nageih, yzy_sheep, and Young912!
- New Chinese Traditional translations by Leo_768 and BoredYukolin!
- New Danish translations by Libreh!
- New French translations by Yosh64, Guillarda, and Narkotiqu!
- New Portuguese translations by Lucasofchirst!
- New Hungarian translations by petya0927 and Remarci!
- New Russian translations by Mrpavel!
- New Turkish translations by Erkin_!
  
