<template>
	<div
		class="dropTarget column"
		:style="{ 'grid-template-columns': `repeat(${props.columns || 1}, minmax(0, 1fr))` }"
		:data-dragged-over="isDraggedOver" ref="dropTargetRef"
	>
		<template v-for="(item, i) in props.items">
			<ItemGroup
				v-if="shouldFilter(item) && item.includes('|')"
				:name="item"
				@item-changed="(newItem) => onItemChanged(i, newItem)"
				@item-removed="onItemRemoved(i)"
			/>
			<Item
				v-if="shouldFilter(item) && !item.includes('|')"
				:can-drag="props.canDrag"
				:can-drop="props.canGroup"
				:name="item"
				@item-changed="(newItem) => onItemChanged(i, newItem)"
				@item-removed="onItemRemoved(i)"
			/>
		</template>
	</div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { useDragTarget } from "../compositions/useDragTarget";
import Item from "./Item.vue";
import ItemGroup from "./ItemGroup.vue";

const props = defineProps<{
	items: string[],
	canDrag?: boolean,
	canDrop?: boolean,
	canGroup?: boolean,
	alwaysDrop?: boolean,
	filter?: string,
	columns?: number,
}>();

function shouldFilter(item: string) {
	return !props.filter || item.includes(props.filter.toLowerCase().trim().replace(/ /g, "_"));
}

const emit = defineEmits<{
	(event: 'itemsChanged', items: string[]): void,
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
		if (!data || !data.name) return;

		const parentElement = dropTargetRef.value!!;
		let index = 0;
		for (let i = 0; i < parentElement.children.length; i++) {
			const element = parentElement.children[i] as HTMLElement;
			if (e.offsetY > element.offsetTop)
				index = i+1;
		}

		const newItems = props.items.slice();
		newItems.splice(index, 0, data.name);
		emit("itemsChanged", [...new Set(newItems)]);
	});
}

/**
 * When an nested item value changes, such as an item being
 * dragged onto another item to create a group.
 */
function onItemChanged(i: number, newItem: string) {
	const newItems = props.items.slice();
	newItems[i] = newItem;
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
