### New Features:

- **Redesigned the lobby menu!** It looks shiny and is hopefully more intuitive for new players. No more floating text!

- **Added a "Consume Items" mode**, which takes items from your inventory when scored ([#137](https://gitlab.com/horrific-tweaks/bingo/-/issues/137))

- **Improved the team chat behavior**, which now:
    * Uses the server-configured [chat type](https://minecraft.wiki/w/Chat_type) formatting
    * Has a few config properties in `config.json`, such as `defaultToTeamChat`
    * Properly implements chat verification

- **Removed the max 8-team limitation** and added support for custom teams with the vanilla `/team` commands ([#33](https://gitlab.com/horrific-tweaks/bingo/-/issues/33))

  Beyond 13 teams you'll run out of distinct team colors and might have performance issues. Use at your own risk.

- Moved around a few items/challenges in the default lists

- (Client) Added an unnecessary HUD transition when viewing the card.

### Bugs Squashed:

- Fixed players being stuck unable to respawn if the game resets while on the death screen ([#139](https://gitlab.com/horrific-tweaks/bingo/-/issues/139))

- Fixed "leading team" being incorrectly displayed in certain tie scenarios ([#138](https://gitlab.com/horrific-tweaks/bingo/-/issues/138))

- Significantly improved reset performance to avoid timeouts ([#136](https://gitlab.com/horrific-tweaks/bingo/-/issues/136))

- Fixed the mod not persisting some game options for the first round after the server is started

- When using [Vanish](https://modrinth.com/mod/vanish), spectators no longer create extra join/leave messages when logging in

- (Client) Card HUD and hint tooltips no longer overlap each other in the lobby ([#140](https://gitlab.com/horrific-tweaks/bingo/-/issues/140))

### Translations

- New Chinese Simplified translations by Young912!
- New Russian translations by Ofterkino, Mrpavel, Май, Lucky Wayч, and yukkovna!
- New Swedish translations by Vica50 Rosvall!