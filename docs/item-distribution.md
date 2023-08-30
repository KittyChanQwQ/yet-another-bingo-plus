<script setup>
import ItemGroup from './components/ItemGroup.vue';
</script>

# Item Distribution

The "item distribution" can be configured in-game to change how many items will be pulled from each tier to make the
card. These values must add up to 25.

![An item distribution menu, showing several tiers with numbers underneath them.](./item-distribution.png)

## Item Groups

In the tier list editor, dragging an item into another item will create an "item group":

<div style="max-width: 280px;">
    <ItemGroup name="pointed_dripstone|dripstone_block" />
</div>

In the text file, this will split the items with a "|" character on the same line - for example: `POINTED_DRIPSTONE|DRIPSTONE_BLOCK`.

When generating a card, only one item from each line can be chosen at a time. This means that, with a group of e.g.
"pointed dripstone" and "dripstone block", you will never see a card contain both items at once.
