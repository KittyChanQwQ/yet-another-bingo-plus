# Yet Another Minecraft BINGO

[![Available on Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/mod/yet-another-minecraft-bingo)
[![Requires Fabric API](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/requires/fabric-api_vector.svg)](https://modrinth.com/mod/fabric-api)

This is a server-side BINGO item hunt mod + lobby, intended for use with vanilla clients. Clients do not need to install
any mods or resource packs to join; this only needs to be installed on the server (along with [fabric-api](https://modrinth.com/mod/fabric-api)).

> ⚠⚠⚠ **WARNING** ⚠⚠⚠
>
> This mod **deletes the world folder on startup**! It is meant to be hosted on a dedicated server for short, temporary
games. Make sure this is not installed on an existing world or you _will_ lose your data!

Recommended mods (optional, but have been tested for compatibility):
- [Lithium](https://modrinth.com/mod/lithium)
- [Krypton](https://modrinth.com/mod/krypton)
- [MC-249136 Fix](https://modrinth.com/mod/mc-249136-fix)
- [Third Person Maps](https://modrinth.com/mod/third-person-maps) (client-side)

Up to 8 teams can compete to collect items on the BINGO card. Teams can score items in horizontal, vertical, or diagonal
lines to win the game. (the "win condition" can be set to a number of lines, or the full card).

![A screenshot of Minecraft plains/ocean with a BINGO card in the corner](https://gitlab.com/horrific-tweaks/bingo/-/raw/main/docs/screenshot.png)

> **Note:** When in-game, you can right-click to "use" the card when it's in your main hand,
> which will display the card items in an inventory menu. This can help if any of the
> images drawn on the card are unclear.

### Gamemodes:

- **Standard**: Collecting an item scores the tile. Normal BINGO behavior.
- **Lockout**: Once an item is collected by any team, that tile is locked and no other teams can collect it.
- **Inventory**: Items must remain in a team member's inventory to hold a tile. Losing an item un-marks its tile on the
  card (meaning you need to stay alive with your items in order to win).

### Extra Options:

- **Unlock Recipes**: Gives players all crafting recipes at the start of the game.
- **Keep Inventory**: Sets the `keepInventory` gamerule. (players keep their items on respawn)
- **Elytra**: Gives all players an elytra and infinite rockets throughout the game.
- **Allow PvP**: Toggles whether players can attack each other.
- **Spawn Kit**: Give teams or players a configurable set of starting gear.

## 📖 Configuration

On the first startup, the mod will create its initial config files in `./config/bingo/...`. No initial setup is needed
to use the mod; it should run out of the box.

The items used to generate bingo card items at different tiers can be customized in `d_tier.txt`, `c_tier.txt`,
`b_tier.txt`, etc. The game will pick each tier item from a random line in its file when generating a card. Items can be
placed on the same line (separated by a `|` character) to ensure that only one of them will appear at a time.

> There's a nifty [configuration tool](https://horrific-tweaks.gitlab.io/bingo/config.html) I've set up that'll let you
> change these tier lists in a drag-and-drop interface rather than editing these files in text.
>
> It's still a bit finicky, but helps prevent typos and misclicks/etc from causing issues...

The item images used to render the BINGO map can be overridden by placing a lowercase named 16x16 PNG file for each item
in the `./config/bingo/images/...` folder. For example, `./config/bingo/images/birch_trapdoor.png`.

The configured game rules (win condition, item distribution) will be saved in `./config/bingo/config.json`. By default,
changing these in-game will overwrite this file. If you'd prefer to have the same default rules on each startup, this
can be turned off by setting `"overwrite": false`.

## ⌨️ Commands

Global commands:
- `/coords` prints the current player coordinates in the team chat
- `/coords list` displays a list of the team's previous "/coords" messages
- `/all` or `/g` sends a message to the global chat (sending a regular chat message defaults to team chat when in-game)

BINGO commands (operator-only):
- `/bingo elytra <true/false>` changes the elytra mode setting
- `/bingo timelimit <minutes>` sets the time limit of the game, in minutes (or "off")
- `/bingo wincon <option>` sets the condition for a team to win
- `/bingo itemdist <distribution...>` changes the item distribution used to generate the card
- `/bingo reroll` rerolls for a new BINGO card with different items
- `/bingo shuffleteams <number>` randomly sorts all players onto the given number of teams
- `/bingo end` ends the current game and displays the winner (or a draw)

(operators can also move specific players to different teams using the vanilla `/team join <team> <player>` command)

## Credits

Parts of this mod are derived/inspired/ported from [Extremelyd1/minecraft-bingo](https://github.com/Extremelyd1/minecraft-bingo),
the awesome Spigot/Paper version of this gamemode.

## License

```
    Copyright (C) 2023 James Fenn

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
```
