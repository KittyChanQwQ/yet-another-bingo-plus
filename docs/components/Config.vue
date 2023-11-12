<template>
	<div style="display: flex; align-items: center; gap: 12px; margin: 12px 0;">
		<DownloadButton :tierlist="tierList" />
		<UploadButton />
	</div>

	<div style="display: flex; align-items: center;">
		<SearchInput @change="onFilterChanged" />
		<div style="flex-grow: 1;"></div>
		<div>
			<input id="unobtainableCheckbox" type="checkbox" v-model="shouldIncludeUnobtainable">
			<label for="unobtainableCheckbox">Include unobtainable items</label>
		</div>
	</div>
	<div style="max-height: 40vh; overflow: auto; margin: 12px 0;">
		<Table
			:columns="['all']"
			:headers="['All Items']"
		>
			<template #all>
				<ItemList
					:items="allItems"
					:columns="5"
					:filter="filter"
					can-drag
					can-drop
					always-drop
				/>
			</template>
		</Table>
	</div>
	<Tabs
		@tab="(newTab) => currentTab = newTab"
		:tabs="['tiers', 'groups']"
		:headers="['Tier Lists', 'Item Groups']"
	>
		<template #tiers>
			<div style="max-height: 40vh; overflow: auto; margin: 12px 0;">
				<Table
					:columns="[...tiers]"
					:headers="tiers.map(tier => `${tier} Tier`)"
				>
					<template v-for="tier in tiers" v-slot:[tier]>
						<ItemList
							can-drag
							can-drop
							:items="tierList[tier]"
							:filter="filter"
							@items-changed="(items) => onItemsChanged(tier, items)"
						/>
					</template>
				</Table>
			</div>
		</template>
		<template #groups>
			<div style="max-height: 40vh; overflow: auto; margin: 12px 0;">
				<Table
					:columns="['groups']"
					:headers="['Item Groups']"
				>
					<template #groups>
						<ItemGroupList
							can-drag
							can-drop
							can-group
							:items="tierList.groups"
							:columns="5"
							:filter="filter"
							@items-changed="(items) => onGroupsChanged(items)"
						/>
					</template>
				</Table>
			</div>
		</template>
	</Tabs>

	<button class="button danger" @click="onReset">Reset</button>

	<details>
		<summary>Legacy tier list upload (.txt)</summary>
		<LegacyUploadItemsInput
			v-for="tier in tiers"
			:label="tier.toLowerCase() + '_tier.txt'"
			@items-uploaded="(items) => onLegacyItems(tier, items)"
		/>
	</details>
</template>

<script setup lang="ts">
import { computed, ref, Ref, onMounted } from "vue";
import ItemList from './ItemList.vue';
import itemData from "../../scripts/data/items.json";
import LegacyUploadItemsInput from "./LegacyUploadItemsInput.vue";
import DownloadButton from "./DownloadButton.vue";
import SearchInput from "./SearchInput.vue";
import Table from "./Table.vue";
import Tabs from "./Tabs.vue";
import { TierList } from "./types";
import UploadButton from "./UploadButton.vue";
import ItemGroupList from "./ItemGroupList.vue";

const TIERLIST_KEY = "tierlist";

const currentTab = ref("tiers");
const shouldIncludeUnobtainable = ref(false);

const unobtainableItems = [
	"minecraft:air",
	"minecraft:barrier",
	"minecraft:bedrock",
	/_spawn_egg$/,
	"minecraft:bundle",
	/command_block/,
	"minecraft:chorus_plant",
	"minecraft:debug_stick",
	"minecraft:dirt_path",
	"minecraft:end_portal_frame",
	"minecraft:farmland",
	"minecraft:frogspawn",
	"minecraft:jigsaw",
	"minecraft:knowledge_book",
	"minecraft:light",
	"minecraft:petrified_oak_slab",
	"minecraft:reinforced_deepslate",
	"minecraft:spawner",
	/structure/,
];

const tiers = ['S', 'A', 'B', 'C', 'D'] as const;

const tierList: Ref<TierList> = ref({
	S: [], A: [], B: [], C: [], D: [],
	groups: [],
});

onMounted(() => {
	// load saved items from localStorage when mounted
	const storedItems = localStorage.getItem(TIERLIST_KEY);
	if (storedItems) {
		tierList.value = JSON.parse(storedItems);
	}
});

const allItems = computed(() => {
	// the "allItems" column should only show items that aren't included in any other tiers
	const items = itemData
		.filter(item => currentTab.value === "tiers" ? !tiers.some(tier => tierList.value[tier].includes(item)) : !tierList.value.groups.some(group => group.includes(item)))
		.filter(item => shouldIncludeUnobtainable.value || !unobtainableItems.some(unobtainable => typeof unobtainable === "string" ? unobtainable === item : new RegExp(unobtainable).test(item)));
	return items;
});

function onItemsChanged(tier: string, newItems: string[]) {
	// update the tierLists ref
	tierList.value[tier] = [...new Set(newItems)];
	// write items to localStorage
	localStorage.setItem(TIERLIST_KEY, JSON.stringify(tierList.value));
}

function onGroupsChanged(newGroups: string[][]) {
	tierList.value.groups = newGroups;
	// write items to localStorage
	localStorage.setItem(TIERLIST_KEY, JSON.stringify(tierList.value));
}

const filter = ref<string>();

function onLegacyItems(tier: keyof TierList, upload: string[]) {
	const groups = upload.filter(row => row.includes("|"));
	const items = upload.filter(row => !row.includes("|"));

	onItemsChanged(tier, [
		...items,
		...groups.flatMap(group => group.split("|")),
	].map(item => "minecraft:" + item.toLowerCase()));

	onGroupsChanged([
		...tierList.value.groups,
		...groups.map(group => {
			return group.split("|").map(item => "minecraft:" + item.toLowerCase());
		}),
	]);
}

function onFilterChanged(value: string) {
	filter.value = !!value.trim() ? value : undefined;
}

function onReset() {
	if (!confirm("This will delete all configured item tiers and groups. Are you sure?"))
		return;

	for (const tier of tiers) {
		onItemsChanged(tier, []);
	}
	onItemsChanged("groups", []);
}
</script>

<style scoped lang="scss">
table {
	overflow-x: auto;
	margin: 0;
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

.button {
	padding: 4px 8px;
	border-radius: 6px;
}

.button.danger {
	background-color: #83261a;
	color: #fff;
}
</style>
