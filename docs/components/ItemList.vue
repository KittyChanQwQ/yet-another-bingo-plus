<template>
	<div
		class="dropTarget column"
		:style="{ 'grid-template-columns': `repeat(${props.columns || 1}, minmax(0, 1fr))` }"
		:data-dragged-over="isDraggedOver" ref="dropTargetRef"
	>
		<template v-for="(item, i) in props.items">
			<template v-if="shouldFilter(item)">
				<ItemGroup
					v-if="(item instanceof Array)"
					:can-drag="props.canDrag"
					:can-drop="props.canGroup"
					:items="item"
					@item-changed="(newItem) => onItemChanged(i, newItem)"
					@item-removed="onItemRemoved(i)"
				/>
				<Item
					v-if="(typeof item === 'string')"
					:can-drag="props.canDrag"
					:can-drop="props.canGroup"
					:name="item"
					@item-changed="(newItem) => onItemChanged(i, newItem)"
					@item-removed="onItemRemoved(i)"
				/>
			</template>
		</template>
	</div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useDragTarget } from "../compositions/useDragTarget";
import Item from "./Item.vue";
import type { TierListEntry } from "./types";
import ItemGroup from "./ItemGroup.vue";

const props = defineProps<{
	items: TierListEntry[],
	canDrag?: boolean,
	canDrop?: boolean,
	canGroup?: boolean,
	alwaysDrop?: boolean,
	filter?: string,
	columns?: number,
}>();

function shouldFilterItem(item: string) {
	return !props.filter || item.includes(props.filter.toLowerCase().trim().replace(/ /g, "_"));
}

// Return 'true' if the item should be visible based on the current 'props.filter'
function shouldFilter(item: TierListEntry) {
	if (item instanceof Array) {
		return item.some((str) => shouldFilterItem(str))
	} else {
		return shouldFilterItem(item);
	}
}

const emit = defineEmits<{
	(event: 'itemsChanged', items: TierListEntry[]): void,
}>();

const dropTargetRef = ref<HTMLElement>();

let isDraggedOver = ref(false);
if (props.canDrop) {
	const { isDraggedOver: isDraggedOverRef, onDrop } = useDragTarget({
		element: dropTargetRef,
		filter(data) {
			return props.alwaysDrop || !props.items.includes(data?.name);
		},
	});

	isDraggedOver = isDraggedOverRef;

	/**
	 * When a new item is dropped onto the list
	 */
	onDrop((data, e) => {
		if (!data || !data.type) return;

		// Gets the index that the item was dropped between...
		/*const parentElement = dropTargetRef.value!!;
		let index = 0;
		for (let i = 0; i < parentElement.children.length; i++) {
			const element = parentElement.children[i] as HTMLElement;
			if (e.offsetY > element.offsetTop)
				index = i+1;
		}*/

		const newItems = props.items.slice();
		if (data.type === "item" && typeof data.name === "string") {
			newItems.push(data.name);
		} else if (data.type === "group" && data.items instanceof Array) {
			newItems.push(data.items);
		}

		emit("itemsChanged", [...new Set(newItems)].sort());
	});
}

/**
 * When an nested item value changes, such as an item being
 * dragged onto another item to create a group.
 */
function onItemChanged(i: number, newItem: TierListEntry) {
	const newItems = props.items.slice();

	// if the item was a group, but now only contains one item, change to a single item
	if (newItem instanceof Array && newItem.length == 1)
		newItems[i] = newItem[0];
	else newItems[i] = newItem;

	emit("itemsChanged", newItems);
}

/**
 * When an item is removed from the list; e.g. being dragged to another group.
 */
function onItemRemoved(i: number) {
	const newItems = props.items.slice();
	newItems.splice(i, 1);
	emit("itemsChanged", newItems);
}
</script>

<style scoped>
.column {
	display: grid;
	grid-template-columns: repeat(1, minmax(0, 1fr));
	grid-auto-rows: min-content;
	gap: 0 8px;

	border: 1px solid var(--vp-c-divider);
}

.dropTarget {
	padding: 8px 16px;
	min-height: 100px;

	transition: background-color .25s, box-shadow .25s;
}

.dropTarget[data-dragged-over="true"] {
	background-color: var(--vp-c-brand-3);
	box-shadow: inset 0 0 0 4px var(--vp-c-brand-1);
}
</style>
