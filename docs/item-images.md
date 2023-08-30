# Item Images

Items will always be drawn onto the bingo card map by the server. This means that the server needs to provide images
for every item that can be drawn on the card.

> In the code:
> [BingoMapService.kt](https://gitlab.com/horrific-tweaks/bingo/-/blob/main/src/main/java/me/jfenn/bingo/map/BingoMapService.kt?ref_type=heads#L59)
> is responsible for drawing the bingo map whenever an update occurs.

These are packaged with the mod by default. However, they can be overridden by placing image files in the
`./config/bingo/images/` directory. These images must be 16x16 PNG files.

For example, `./config/bingo/images/acacia_trapdoor.png` will override the image for the "acacia trapdoor" item. These
must match its item id, which can be checked in the tier list or in-game (using F3+H to see the item ids in tooltips).
