<table>
<tbody>
<td>

### Looking for a modpack?

Check out [Yet Another BINGO: Ultimate](https://modrinth.com/modpack/yet-another-bingo-ultimate) for an opinionated collection of helpful bingo mods!

</td>
</tbody>
</table>

![Yet Another Bingo for Minecraft](https://gitlab.com/horrific-tweaks/bingo/-/raw/main/docs/banner-with-card.png)

[![Requires Fabric API](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/requires/fabric-api_vector.svg)](https://modrinth.com/mod/fabric-api)
[![Available on GitLab](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/available/gitlab_vector.svg)](https://gitlab.com/horrific-tweaks/bingo)
[![Available on Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/mod/yet-another-minecraft-bingo)

*For an introduction to Minecraft BINGO, read "[What is Minecraft BINGO?](https://minecraft.horrific.dev/bingo/what-is-bingo.html)" on the wiki!*

This is a multiplayer BINGO/Lockout mod + lobby, which works on the client or on a dedicated server.

- When installed on a server, players do not need any mods or resource packs to join.
- When installed on the client, it provides additional functionality for server games, and can run local games in singleplayer or LAN.

<table>
<tbody>
<tr>
<td>
<img alt="A screenshot of Minecraft game with a BINGO card in the corner" src="https://gitlab.com/horrific-tweaks/bingo/-/raw/main/docs/screenshot-with-hud-3.png" />
</td>
<td>

Up to 8 teams can compete to collect items on the BINGO card. Score items in horizontal, vertical, or diagonal
lines to win the game.

---

⌨ Press "Y" to see the Bingo Card in an inventory view!

🖱️ Vanilla clients can right-click when holding the map to do the same.

</td>
</tr>
</tbody>
</table>

### Gamemodes:

- **Standard**: Collecting an item scores the tile. Normal BINGO behavior.
- **Lockout**: Once an item is collected by any team, that tile is locked and no other teams can collect it.
- **Inventory**: Items must remain in a team member's inventory to hold a tile. Losing an item un-marks its tile on the
  card (meaning you need to stay alive with your items in order to win).
- **Hidden Items**: Items are not shown on the bingo card until a player discovers them.
- **Consume Items**: Items are taken from the player whenever they are captured on a card.

### Extra Options:

- **Unlock Recipes**: Gives players all crafting recipes at the start of the game.
- **Keep Inventory**: Sets the `keepInventory` gamerule. (players keep their items on respawn)
- **Elytra**: Gives all players an elytra and infinite rockets throughout the game.
- **Night Vision**: Gives all players the night vision effect. (can be toggled individually with `/bingoprefs`)
- **Allow PvP**: Toggles whether players can attack each other.
- **Spawn Kit**: Give teams or players a configurable set of starting gear.

## 📖 Configuration

*When using this mod in single-player, make sure to install [Mod Menu](https://modrinth.com/mod/modmenu) and [YetAnotherConfigLib](https://modrinth.com/mod/yacl) to use the built-in config screen!*

![A configuration menu for the Bingo GUI options](https://gitlab.com/horrific-tweaks/bingo/-/raw/main/docs/config-menu-3.png)

No initial setup is needed  to use the mod; it should run out of the box. The config files live in
`./config/yet-another-minecraft-bingo`.

The items used to generate bingo card items at different tiers can be customized in the `*.tierlist.json` files. Once
placed in the `./config/yet-another-minecraft-bingo/` directory, they can be enabled in-game using the "Tier List" menu.

> There's a nifty [configuration tool](https://minecraft.horrific.dev/bingo/tier-editor.html) I've set up that'll let you
> change these tier lists in a drag-and-drop interface rather than editing the files in text.

You can find more info about configuring this mod [on the wiki](https://minecraft.horrific.dev/bingo/config.html)!

## ⌨️ Commands

This is a brief summary of the most important commands. See the [Command Cheatsheet](https://minecraft.horrific.dev/bingo/command-cheatsheet) for a complete list of bingo commands and descriptions!

Global commands:
- `/coords` prints the current player coordinates in the team chat
- `/coords list` displays a list of the team's previous "/coords" messages
- `/all` or `/g` sends a message to the global chat (sending a regular chat message defaults to team chat when in-game)
- `/join <team>` (when in the lobby) allows a player to join the specified team

BINGO commands (operator-only):
- `/bingo reroll` rerolls for a new Bingo Card with different items
- `/bingo seed` shows or sets the random seed used to generate the current Bingo Card
- `/bingo end` immediately ends the current game and displays the winner (or a draw)
- `/join <team> <player>` assigns a player to a different team - will respawn them if in-game

## Credits

The very pretty Bingo Card HUD and "Bingo!" button textures were made by [@marceles](https://marceles.carrd.co)!

Parts of this mod are inspired from [Extremelyd1/minecraft-bingo](https://github.com/Extremelyd1/minecraft-bingo), the awesome Spigot/Paper version of this gamemode.

The logo design was inspired by [Aikoyori/ProgrammingVTuberLogos](https://github.com/Aikoyori/ProgrammingVTuberLogos)

Vanilla entity/challenge goal textures are from [minecraft-heads.com](https://minecraft-heads.com/)

### Translations

<table>
<tbody>
<td>

[![Localized on CrowdIn](https://img.shields.io/badge/Localized_On-Crowdin-373d4c?style=for-the-badge&labelColor=464e62&logoColor=white&logo=Crowdin)](https://crowdin.com/project/yet-another-bingo)
<br>**Help translate this mod into your language!** All text in this project is available [on CrowdIn](https://crowdin.com/project/yet-another-bingo) for anyone to submit translations.

</td>
</tbody>
</table>


Chinese translations were contributed by Fishlanding, tovv3r, and Young912!<br>
French translations were contributed by Yosh64!<br>
Polish translations were contributed by Toniemax!<br>
Portuguese translations were contributed by Lucasofchirst!<br>
Russian translations were contributed by Milleol, XakVit, Ofterkino, Mrpavel, Май, Lucky Wayч, and yukkovna!<br>
Spanish translations were contributed by ANDY!<br>
Swedish translations were contributed by Vica50 Rosvall!<br>
