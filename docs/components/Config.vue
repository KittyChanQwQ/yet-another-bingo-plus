<template>
	<SearchInput @change="onFilterChanged" />
	<table>
		<thead>
			<tr>
				<th v-for="tier in tiers">
					<div style="display: flex; align-items: center; gap: 4px;">
						<DownloadItemsInput v-if="!!tierLists[tier].length" :tier="tier" :items="tierLists[tier]" />
						<span style="flex-grow: 1;">{{ tier.toUpperCase() }} Tier</span>
						<UploadItemsInput @items-uploaded="(items) => onItemsChanged(tier, items)" />
					</div>
				</th>
				<th>All Items</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td v-for="tier in tiers">
					<ItemList
						can-drag
						can-drop
						can-group
						:items="tierLists[tier]"
						:filter="filter"
						@items-changed="(items) => onItemsChanged(tier, items)"
					/>
				</td>
				<td>
					<ItemList
						:items="allItems"
						:filter="filter"
						can-drag
						can-drop
					/>
				</td>
			</tr>
		</tbody>
	</table>
</template>

<script setup lang="ts">
import { computed, ref, Ref, onMounted } from "vue";
import ItemList from './ItemList.vue';
import itemData from "../../scripts/data/items.json";
import UploadItemsInput from "./UploadItemsInput.vue";
import DownloadItemsInput from "./DownloadItemsInput.vue";
import SearchInput from "./SearchInput.vue";

const validItemSet = new Set(itemData);

const tiers = ['s', 'a', 'b', 'c', 'd'] as const;

const tierLists: Ref<{ [K in typeof tiers[number]]: string[] }> = ref({
	s: [], a: [], b: [], c: [], d: [],
});

onMounted(() => {
	// load saved items from localStorage when mounted
	for (const tier of tiers) {
		const storedItems = localStorage.getItem(`bingo_${tier}`);
		if (storedItems) {
			tierLists.value[tier] = [...JSON.parse(storedItems)]
				.map(s => {
					return s.trim()
						.toLowerCase()
						.split("|")
						.map(item => item.replace(/^minecraft\:/, ""))
						.filter(item => validItemSet.has(`minecraft:${item}`))
						.join("|");
				})
				.filter(s => !!s.length)
		}
	}
});

const allItems = computed(() => {
	// the "allItems" column should only show items that aren't included in any other tiers
	const items = itemData
		.map(item => item.replace(/^minecraft\:/, ""))
		.filter(item => !Object.values(tierLists.value).some(list => list.some(items => items.split("|").includes(item))));
	return items;
});

function onItemsChanged(tier: typeof tiers[number], newItems: string[]) {
	// update the tierLists ref
	tierLists.value[tier] = newItems;
	// write items to localStorage
	localStorage.setItem(`bingo_${tier}`, JSON.stringify(newItems));
}

const filter = ref<string>();

function onFilterChanged(value: string) {
	filter.value = !!value.trim() ? value : undefined;
}
</script>

<style scoped lang="scss">
table {
	overflow-x: auto;
}

th, td {
	min-width: 150px;
	vertical-align: top;
}

td {
	// this is a hack to get a <div style="height: 100%;"> to fill the table cell
	height: 1px;
	padding: 0;
}
</style>
