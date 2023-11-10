---
layout: doc
---

<script setup>
import Config from './components/Config.vue'
</script>

# Tier List Editor

Use this page to generate your own item tier lists to use with the mod. These files can be downloaded and placed in your server's config directory - located in `./config/bingo/a_tier.txt`:

```txt{4-8}
🗀 config/
 └ 🗀 bingo/
   ├ 🗀 images/
   ├ a_tier.txt
   ├ b_tier.txt
   ├ c_tier.txt
   ├ d_tier.txt
   └ s_tier.txt
🗀 mods/
🗀 world/
server.properties
```

The items below can be dragged between each column to sort them into tiers. Dragging an item back to "all items" will remove it from the list.

<Config />

<style lang="scss">
	.content-container {
		max-width: unset !important;
	}

	.aside {
		display: none !important;
	}

	header {
		// the default "position: fixed;" causes the drag-to-scroll behavior to break over the nav header
		position: sticky !important;
	}

	.VPContent {
		padding-top: 0 !important;
	}
</style>
