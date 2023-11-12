<template>
	<div
		class="tabs"
	>
		<div class="tabsHeader">
			<button
				v-for="(header, index) in headers"
				:class="tab == props.tabs[index] ? 'tab active' : 'tab'"
				@click="() => handleTab(props.tabs[index])"
			>
				{{ header }}
			</button>
		</div>

		<slot :name="tab" />
	</div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const props = defineProps<{
	tabs: string[],
	headers: string[],
}>();

const tab = ref(props.tabs[0]);

const emit = defineEmits<{
	(event: "tab", tab: string): void,
}>();

function handleTab(newTab: string) {
	emit("tab", newTab);
	tab.value = newTab;
}
</script>

<style scoped>
.tabsHeader {
	display: flex;
	align-items: flex-end;
	gap: 8px;
	margin-bottom: 4px;
}

.tab {
	padding: 8px 4px;
	border-bottom: 2px solid transparent;
	color: var(--vp-c-text-2);
}

.tab:hover {
	background-color: var(--vp-c-default-soft);
}

.tab.active {
	border-color: var(--vp-c-brand-3);
	color: var(--vp-c-brand-3);
}
</style>
