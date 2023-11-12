<template>
	<div>
		<a :href="uri" download="default.tierlist.json">
			<Icon icon="ic:baseline-download" width="22" height="22" />
			Save Tier Lists
		</a>
	</div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { Icon } from '@iconify/vue';
import { TierList } from './types';

const props = defineProps<{
	tierlist: TierList,
}>();

const uri = computed(() => {
	const json = JSON.stringify(props.tierlist, null, 4);
	return `data:text/plain;charset=utf-8,${encodeURIComponent(json)}`;
});

</script>

<style scoped>
a {
	display: flex;
	align-items: center;
	gap: 4px;

	cursor: pointer;
	margin: 0;
	padding: 2px 8px;
	text-decoration: none;
	background-color: var(--vp-button-brand-bg);
	color: var(--vp-button-brand-text);
	border: 1px solid var(--vp-button-brand-border);
	border-radius: 6px;
}

a:hover {
	border-color: var(--vp-button-brand-hover-border);
	background-color: var(--vp-button-brand-hover-bg);
	color: var(--vp-button-brand-hover-text);
}

a:active {
	border-color: var(--vp-button-brand-active-border);
	background-color: var(--vp-button-brand-active-bg);
	color: var(--vp-button-brand-active-text);
}
</style>
