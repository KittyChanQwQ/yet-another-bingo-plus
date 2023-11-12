<template>
	<div
		class="item"
		draggable="true"
		:data-dragged-over="isDraggedOver"
		ref="itemRef"
	>
		<p class="title">Item Group</p>
		<Item
			v-for="(item, i) in props.items"
			:name="item"
			can-drag
			@item-removed="onItemRemoved(i)"
		/>
	</div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import Item from "./Item.vue";
import { useDragItem } from "../compositions/useDragItem";
import { useDragTarget } from "../compositions/useDragTarget";

const props = defineProps<{
	items: string[],
}>();

const emit = defineEmits<{
	(event: "itemChanged", items: string[]): void;
	(event: "itemRemoved"): void;
}>();

const itemRef = ref<HTMLElement>();

const { isDragging, onDragEnd } = useDragItem({
	element: itemRef,
	data: computed(() => ({ type: "group", items: props.items })),
});

onDragEnd((isSuccessful) => {
	console.log("<ItemGroup> drag end", { isSuccessful });
	if (isSuccessful) emit("itemRemoved");
});

const { isDraggedOver, onDrop } = useDragTarget({
	element: itemRef,
	action: "move",
	filter(data) {
		console.log("<ItemGroup> filter", data);
		return (data?.type === "group" && data?.items && !data.items.every(value => props.items.includes(value)))
			|| (data?.type === "item" && data.name && !props.items.includes(data.name));
	},
});

onDrop((data) => {
	console.log("<ItemGroup> on drop", data);
	if (data.type === "group") {
		// add new items to the group item
		const dataItems = [...data.items] as string[];
		const newItems = [...new Set([...props.items, ...dataItems])];
		emit("itemChanged", newItems);
	}
	if (data.type === "item") {
		const newItems = [...new Set([...props.items, data.name])];
		emit("itemChanged", newItems);
	}
});

/**
 * When an item is removed from the list; e.g. being dragged to another group.
 */
function onItemRemoved(i: number) {
	const newItems = props.items.slice();
	newItems.splice(i, 1);
	if (newItems.length)
		emit("itemChanged", newItems);
	else emit("itemRemoved");
}
</script>

<style scoped lang="scss">
.item {
	padding: 4px 8px;
	margin: 8px 0;

	background-color: var(--vp-c-bg-soft);
	border: 1px solid var(--vp-c-divider);
	border-radius: 4px;;

	transition: box-shadow .25s, border .25s;

	cursor: move;
}

.item:hover {
	border: 1px solid var(--vp-c-brand-1);
}

.item[data-dragged-over="true"] {
	box-shadow: 0 0 0 4px var(--vp-c-brand-1);
}

.title {
	font-size: 0.8rem;
	margin: 0;
	font-weight: bold;
	color: var(--vp-c-text-2);
}
</style>
