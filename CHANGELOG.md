### 1.2.0.beta3:

- Added a "Game Over" screen that shows a restart / new game prompt with the bingo cards & win reason

- Added a `"shutDownWhenGameOverSeconds"` config to automatically shutdown or restart the server after a game ends, without waiting for players to disconnect. ([#76](https://gitlab.com/horrific-tweaks/bingo/-/issues/76))

- Uses player names instead of team names if there is only one player on the team
  
- Various text changes to support translations in the future ([#79](https://gitlab.com/horrific-tweaks/bingo/-/issues/79))

- Fixed custom screens not rendering a dimmed background on the 1.20.1 build

### New Features:

- Added a client-side HUD to replace the Bingo Card map, if the mod is installed ([#70](https://gitlab.com/horrific-tweaks/bingo/-/issues/70))

  (the mod remains fully compatible with vanilla clients - this is just a QoL feature)

- If [Just Enough Items](https://modrinth.com/mod/jei) is installed, clicking on a Bingo Card item will open its recipe! ([#75](https://gitlab.com/horrific-tweaks/bingo/-/issues/75))

- Added a config GUI (requires [Mod Menu](https://modrinth.com/mod/modmenu) and [YetAnotherConfigLib](https://modrinth.com/mod/yacl)) for game options and player settings

  Player settings in the config screen are automatically synced with any servers you join. ([#74](https://gitlab.com/horrific-tweaks/bingo/-/issues/74))

- Added a player-specific "Night Vision" setting to turn off NightVis on a per-player basis. ([#72](https://gitlab.com/horrific-tweaks/bingo/-/issues/72))

### Bugs Squashed:

- All game sounds (note blocks, button clicks) now play in the expected sound channels
