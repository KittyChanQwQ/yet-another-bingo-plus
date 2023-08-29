<template>
	<div>
		<input :id="inputId" type="file" @change="onChange" accept=".txt" />
		<label :for="inputId" aria-label="Upload" title="Upload">
			<Icon icon="ic:baseline-upload" width="22" height="22" />
		</label>
	</div>
</template>

<script setup lang="ts">
import itemData from "../data/items.json";
import { Icon } from '@iconify/vue';

const inputId = `ID${Math.random()}`;

const validItemSet = new Set(itemData);

const emit = defineEmits<{
	(event: "itemsUploaded", items: string[]): void;
}>();

async function onChange(e: Event) {
	const input = e.target as HTMLInputElement;
	if (!input.files || !input.files.length) return;

	const file = input.files[0];
	const fileText = await file.text();

	const items = fileText.split("\n")
		.map(s => s.trim())
		.map(s => {
			return s.split("|")
				.filter(i => validItemSet.has(`minecraft:${i.toLowerCase()}`))
				.join("|");
		})
		.filter(s => !!s.length)

	emit("itemsUploaded", items);
}

</script>

<style scoped>
label {
	display: block;
	padding: 4px;
	cursor: pointer;
}

input {
	width: 0.1px;
	height: 0.1px;
	opacity: 0;
	overflow: hidden;
	position: absolute;
	z-index: -1;
}

input:focus + label {
	outline: 2px solid var(--vp-c-text-1);
}

input:focus + label,
label:hover {
	color: var(--vp-c-text-1);
}
</style>
