<template>
	<div
		class="item"
		:draggable="props.canDrag"
		:data-dragged-over="isDraggedOver"
		:data-item="props.name"
		ref="itemRef"
	>
		<span class="itemName">
			{{ props.name.replace(/^minecraft\:/, "").replace(/_/g, " ") }}
		</span>
		<span class="itemDropTarget" ref="itemDropTargetRef" v-if="props.canDrop">
			+
		</span>
	</div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { useDragItem } from "../compositions/useDragItem";
import { useDragTarget } from "../compositions/useDragTarget";
import { TierListEntry } from "./types";

const props = defineProps<{
	name: string,
	canDrag?: boolean,
	canDrop?: boolean,
}>();

const emit = defineEmits<{
	(event: "itemChanged", item: TierListEntry): void;
	(event: "itemRemoved"): void;
}>();

const itemRef = ref<HTMLElement>();

if (props.canDrag) {
	const { isDragging, onDragEnd } = useDragItem({
		element: itemRef,
		data: computed(() => ({ type: "item", name: props.name })),
	});

	onDragEnd((isSuccessful) => {
		if (isSuccessful) emit("itemRemoved");
	});
}

let isDraggedOver = ref(false);
const itemDropTargetRef = ref<HTMLElement>();

if (props.canDrop) {
	const { isDraggedOver: isDraggedOverRef, onDrop } = useDragTarget({
		element: itemRef,
		action: "move",
		filter(data) {
			// console.log("<Item> filter", data);
			return data?.type === "item" && data.name && data.name != props.name;
		},
	});

	isDraggedOver = isDraggedOverRef;

	onDrop((data) => {
		// turn the item into a group item
		if (props.name != data.name)
			emit("itemChanged", [props.name, data.name].sort());
	});
}
</script>

<style scoped lang="scss">
.item {
	max-width: 100%;

	display: flex;
	align-items: center;

	padding: 4px 8px;
	margin: 4px 0;

	background-color: var(--vp-c-bg-soft);
	border: 1px solid var(--vp-c-divider);
	border-radius: 4px;

	transition: border .25s;

	cursor: move;
	user-select: none;
}

.itemName {
	font-size: 0.8rem;
	line-height: 1rem;
	flex-grow: 1;
}

.item:hover {
	border: 1px solid var(--vp-c-brand-1);
}

.item[data-dragged-over="true"] {
	box-shadow: 0 0 0 4px var(--vp-c-brand-1);
}

.itemDropTarget {
	opacity: 0;
	z-index: 1000;

	transition: opacity .25s;
}

.item[data-dragged-over="true"] .itemDropTarget {
	opacity: 1;
}
</style>
