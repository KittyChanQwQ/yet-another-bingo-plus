# Yet Another Minecraft BINGO

[![Requires Fabric API](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/requires/fabric-api_vector.svg)](https://modrinth.com/mod/fabric-api)
[![Available on GitLab](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/available/gitlab_vector.svg)](https://gitlab.com/horrific-tweaks/bingo)
[![Available on Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/mod/yet-another-minecraft-bingo)

*For an introduction to Minecraft BINGO, read "[What is Minecraft BINGO?](https://minecraft.horrific.dev/bingo/what-is-bingo.html)" on the wiki!*

This is a multiplayer BINGO item hunt mod + lobby, which works on the client or on a dedicated server.

- When installed on a server, players do not need any mods or resource packs to join.
- When installed on the client, it provides additional functionality for server games, and can run games in singleplayer or on LAN.

```txt
⚠️⚠️⚠️ WARNING ⚠️⚠️⚠️

This mod **deletes the world folder** when installed on a server!
It is meant to be hosted on a dedicated server for short, temporary games.
Make sure this is not installed on an existing world or you will lose your data!
```

<table>
<tbody>
<tr>
<td>
<img alt="A screenshot of Minecraft plains/ocean with a BINGO card in the corner" src="https://gitlab.com/horrific-tweaks/bingo/-/raw/main/docs/screenshot.png" />
</td>
<td>

Up to 8 teams can compete to collect items on the BINGO card. Teams can score items in horizontal, vertical, or diagonal
lines to win the game.

---

🖱️ Right-click when holding the map to see the items in an inventory view!

</td>
</tr>
</tbody>
</table>

### Gamemodes:

- **Standard**: Collecting an item scores the tile. Normal BINGO behavior.
- **Lockout**: Once an item is collected by any team, that tile is locked and no other teams can collect it.
- **Inventory**: Items must remain in a team member's inventory to hold a tile. Losing an item un-marks its tile on the
  card (meaning you need to stay alive with your items in order to win).
- **Ranked**: The game does not end when a team wins. Instead, it keeps going until the time limit and lists the winners
  when it ends.

### Extra Options:

- **Unlock Recipes**: Gives players all crafting recipes at the start of the game.
- **Keep Inventory**: Sets the `keepInventory` gamerule. (players keep their items on respawn)
- **Elytra**: Gives all players an elytra and infinite rockets throughout the game.
- **Allow PvP**: Toggles whether players can attack each other.
- **Spawn Kit**: Give teams or players a configurable set of starting gear.
- **Hidden Items**: Items are not shown on the bingo card until a player discovers them.

## 📖 Configuration

On the first startup, the mod will create its initial config files in `./config/bingo/...`. No initial setup is needed
to use the mod; it should run out of the box.

The items used to generate bingo card items at different tiers can be customized in the `*.tierlist.json` files. Once
placed in the `./config/bingo/` directory, they can be enabled in-game using the "Tier List" menu.

> There's a nifty [configuration tool](https://minecraft.horrific.dev/bingo/tier-editor.html) I've set up that'll let you
> change these tier lists in a drag-and-drop interface rather than editing the files in text.

You can find more info about configuring this mod [on the wiki](https://minecraft.horrific.dev/bingo/config.html)!

## ⌨️ Commands

Global commands:
- `/coords` prints the current player coordinates in the team chat
- `/coords list` displays a list of the team's previous "/coords" messages
- `/all` or `/g` sends a message to the global chat (sending a regular chat message defaults to team chat when in-game)
- `/join <team>` (when in the lobby) allows a player to join the specified team

BINGO commands (operator-only):
- `/bingo elytra <true/false>` changes the elytra mode setting
- `/bingo timelimit <minutes>` sets the time limit of the game, in minutes (or "off")
- `/bingo wincon <option>` sets the condition for a team to win
- `/bingo itemdist <distribution...>` changes the item distribution used to generate the card
- `/bingo reroll` rerolls for a new Bingo Card with different items
- `/bingo seed` shows the random seed used to generate the current Bingo Card
- `/bingo seed <number>` sets the Bingo Card seed to a specific value (which will generate the same card as long as the settings are not changed)
- `/bingo shuffleteams <number>` randomly sorts all players onto the given number of teams
- `/bingo end` ends the current game and displays the winner (or a draw)
- `/join <team> <player>` assigns a player to a different team - will respawn them if in-game

## Credits

Parts of this mod are inspired from [Extremelyd1/minecraft-bingo](https://github.com/Extremelyd1/minecraft-bingo),
the awesome Spigot/Paper version of this gamemode.
