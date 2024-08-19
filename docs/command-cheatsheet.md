---
description: A reference page for all of the game commands provided in Yet Another Minecraft BINGO.
---

# Command Cheatsheet

## Player commands

| Command                             | Description                                                                                                         |
|-------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| `/coords`                           | Prints the player's current coordinates in the team chat.                                                           |
| `/coords list`                      | Displays a list of the team's previous "/coords" messages.                                                          |
| `/all` or `/g`                      | Sends a message outside of the team chat.                                                                           |
| `/spectator`                        | Toggle between creative/spectator gamemodes after a game (or when spectating a game, if Vanish is installed).       |
| `/bingoprefs <option> <true/false>` | Changes a player setting (e.g. `scoreMessages` or `scoreboardAutoHide`). These options are also shown in the lobby. |
| `/bingo`                            | Summarizes the current game configuration.                                                                          |

## Operator-only commands

| Command                              | Description                                                                            |
|--------------------------------------|----------------------------------------------------------------------------------------|
| `/bingo start`                       | Starts a game (if there isn't already one running).                                    |
| `/bingo end`                         | Immediately ends the current game and displays the winner.                             |
| `/bingo shuffleteams #`              | Randomly shuffles all players onto the given number of teams.                          |
| `/bingo elytra`                      | Changes whether players have elytra/rockets during the game.                           |
| `/bingo night_vision`                | Changes whether players are given the night vision effect during the game.             |
| `/bingo timelimit #`                 | Sets the time limit of the game, in minutes (or "off" for no timer)                    |
| `/bingo mode <option>`               | Toggles on/off the different Bingo modes, such as Lockout, Inventory, or Hidden Items. |
| `/bingo goal <option>`               | Changes the number of Bingo lines needed for a team to win the game.                   |
| `/bingo itemdist # # # # #`          | Changes the [item distribution](./item-distribution) used to generate the card.        |
| `/bingo tierlist <name>`             | Changes the tier list used to generate the card.                                       |
| `/bingo reroll`                      | Re-rolls the card for completely new random items.                                     |
| `/bingo card <tile> set <objective>` | Manually changes the specified tile on the Bingo Card.                                 |
| `/bingo seed`                        | Shows the random seed used to generate the current Bingo Card.                         |
| `/bingo seed #`                      | Sets the Bingo Card seed to a specific value.                                          |
| `/join <team> <player>`              | Assigns a player to a different team - will respawn them if in-game.                   |
