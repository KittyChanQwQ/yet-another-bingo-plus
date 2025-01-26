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

- (Client) Tiles with multiple items now display each item as part of the hover tooltip.

- (Client) 'Game over' screen now remembers which tab it was on, and only renders the win firework the first time it's opened.

### Bugs Squashed:

- Fixed the card occasionally generating with blank spaces when certain filters are applied

- Fixed HUD desync with [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) when relogging

- Fix occasional "{team} completed their card!" messages when starting a new game

- Fixed compatibility with new versions of [Just Enough Items](https://modrinth.com/mod/jei)

### Translations

- New Chinese Simplified translations by Young912!
- New Danish translations by Libreh!
- New Portuguese translations by Lucasofchirst!
- New Hungarian translations by petya0927!
