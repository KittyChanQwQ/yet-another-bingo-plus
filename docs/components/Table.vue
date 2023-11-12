<template>
	<div
		class="table"
		:style="{
			'grid-template-columns': `repeat(${props.columns.length}, minmax(0, 1fr))`,
		}"
	>
		<div v-for="header in headers" class="columnHeader">
			{{ header }}
		</div>

		<slot v-for="column in columns" :name="column" />
	</div>
</template>

<script setup lang="ts">
const props = defineProps<{
	columns: string[],
	headers: string[],
}>();
</script>

<style scoped>
.table {
	display: grid;
}

.columnHeader {
	text-align: left;
    font-size: 14px;
    font-weight: 600;
    color: var(--vp-c-text-2);
    background-color: var(--vp-c-bg-soft);

	border: 1px solid var(--vp-c-divider);
    padding: 8px 16px;
	position: sticky;
	top: 0;
}
</style>
