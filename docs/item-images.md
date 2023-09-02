# Item Images

Items will always be drawn onto the bingo card map by the server. This means that the server needs to provide images
for every item that can be drawn on the card.

> In the code:
> [MapRenderService.kt](https://gitlab.com/horrific-tweaks/bingo/-/blob/main/src/main/java/me/jfenn/bingo/map/MapRenderService.kt)
> is responsible for drawing the bingo map whenever an update occurs.

These are packaged with the mod by default. However, they can be overridden by placing image files in the
`./config/bingo/images/` directory. These images must be 16x16 PNG files.

For example, `./config/bingo/images/acacia_trapdoor.png` will override the image for the "acacia trapdoor" item. These
must match its item id, which can be checked in the tier list or in-game (using F3+H to see the item ids in tooltips).

## Color Accuracy

Minecraft maps are represented by a `byte[]` of color indexes, which correspond to this list as described in [the Minecraft wiki](https://minecraft.fandom.com/wiki/Map_item_format#Base_colors).

In order to render item images, we need to pick the closest map color to each pixel. Currently this is achieved with a
bruteforce method of comparing each possible color index to the desired color, and finding the closest value.

To measure the similarity between two colors, I'm using a combination of two measurements that seemed to produce the
most intuitive results: a reliable approximation based on Euclidian distance (described [in this paper](https://www.compuphase.com/cmetric.htm))
and [CMC l:c (1984) formula](https://en.wikipedia.org/wiki/Color_difference#CMC_l:c_(1984)). Neither of these provide
perfect results for every item image, but I've settled on the current comparison being "good enough".
