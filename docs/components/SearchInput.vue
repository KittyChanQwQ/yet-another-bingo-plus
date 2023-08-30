<template>
	<div class="input">
		<Icon icon="mdi:search" width="18" height="18" />
		<input type="search" placeholder="Search..." @input="onChange">
	</div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue';

const emit = defineEmits<{
	(event: "change", value: string): void;
}>();

function onChange(e: Event) {
	const element = e.target as HTMLInputElement;
	emit("change", element.value);
}
</script>

<style>
.input {
	display: inline-flex;
	align-items: center;
	background-color: var(--vp-c-bg-soft);
	gap: 8px;
	padding: 0 8px;

	border: 1px solid var(--vp-c-divider);
	transition: border .25s;
}

input {
	padding: 8px 0;
}

.input:focus-within {
	border: 1px solid var(--vp-c-brand-1);
}
</style>
