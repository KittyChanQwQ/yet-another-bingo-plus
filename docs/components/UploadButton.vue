<template>
	<div>
		<input :id="inputId" type="file" @change="onChange" accept=".tierlist.json" />
		<label :for="inputId" aria-label="Upload" title="Upload">
			<Icon icon="ic:baseline-upload" width="22" height="22" />
			Upload Tier Lists
		</label>
	</div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue';
import { TierList } from "./types";

const inputId = `ID${Math.random()}`;

const emit = defineEmits<{
	(event: "listUploaded", list: TierList): void;
}>();

async function onChange(e: Event) {
	const input = e.target as HTMLInputElement;
	if (!input.files || !input.files.length) return;

	const file = input.files[0];
	const fileText = await file.text();

	const list: TierList = JSON.parse(fileText);

	emit("listUploaded", list);
}

</script>

<style scoped>
label {
	display: flex;
	align-items: center;
	gap: 4px;

	cursor: pointer;
	margin: 0;
	padding: 2px 8px;
	text-decoration: none;
	background-color: var(--vp-button-alt-bg);
	color: var(--vp-button-alt-text);
	border: 1px solid var(--vp-button-alt-border);
	border-radius: 6px;
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
	border-color: var(--vp-button-alt-hover-border);
	background-color: var(--vp-button-alt-hover-bg);
	color: var(--vp-button-alt-hover-text);
}
</style>
