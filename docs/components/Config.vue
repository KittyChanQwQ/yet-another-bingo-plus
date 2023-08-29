<template>
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
				<ItemList
					v-for="tier in tiers"
					can-drag
					can-drop
					:items="tierLists[tier]"
					@items-changed="(items) => onItemsChanged(tier, items)"
				/>
				<ItemList :items="allItems" can-drag />
			</tr>
		</tbody>
	</table>
</template>

<script setup lang="ts">
import { computed, ref, Ref } from "vue";
import ItemList from './ItemList.vue';
import itemData from "../data/items.json";
import UploadItemsInput from "./UploadItemsInput.vue";
import DownloadItemsInput from "./DownloadItemsInput.vue";

const tiers = ['s', 'a', 'b', 'c', 'd'] as const;

const tierLists: Ref<{ [K in typeof tiers[number]]: string[] }> = ref({
	s: [], a: [], b: [], c: [], d: [],
});

const allItems = computed(() => {
	const items = itemData.filter(item => !Object.values(tierLists.value).some(list => list.some(items => items.split("|").includes(item))));
	return items;
});

function onItemsChanged(tier: typeof tiers[number], newItems: string[]) {
	tierLists.value[tier] = newItems;
}
</script>

<style scoped>
table {
	overflow-x: scroll;
}

th, td {
	min-width: 150px;
	vertical-align: top;
}
</style>
