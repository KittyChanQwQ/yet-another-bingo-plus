# Minecraft BINGO

> **⚠ WARNING**
>
> This mod **deletes the world folder on startup**! It is meant to be hosted on a dedicated server for short, temporary
games. Make sure this is not installed on an existing world or you _will_ lose your data!

## 📖 Configuration

On the first startup, the mod will create its initial config files in `./config/bingo/...`. No initial setup is needed
to use the mod; it should run out of the box.

The items used to generate bingo card items at different tiers can be customized in `d_tier.txt`, `c_tier.txt`,
`b_tier.txt`, etc. The game will pick each tier item from a random line in its file when generating a card. Items can be
placed on the same line (separated by a `|` character) to ensure that only one of them will appear at a time.

The item images used to render the BINGO map can be overridden by placing a lowercase named 16x16 PNG file for each item
in the `./config/bingo/images/...` folder. For example, `./config/bingo/images/birch_trapdoor.png`.

The configured game rules (win condition, item distribution) will be saved in `./config/bingo/config.json`. By default,
changing these in-game will overwrite this file. If you'd prefer to have the same default rules on each startup, this
can be turned off by setting `"overwrite": false`.

## Credits

Parts of this mod are derived/inspired/ported from [Extremelyd1/minecraft-bingo](https://github.com/Extremelyd1/minecraft-bingo),
the Spigot/Paper version of this gamemode.

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
