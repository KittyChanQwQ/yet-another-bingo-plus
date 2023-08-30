<template>
	<div>
		<a :href="uri" :download="`${props.tier}_tier.txt`">
			<Icon icon="ic:outline-save" width="22" height="22" />
		</a>
	</div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { Icon } from '@iconify/vue';

const props = defineProps<{
	tier: string,
	items: string[],
}>();

const uri = computed(() => {
	const items = props.items.map(s => s.replace(/^minecraft\:/, "").toUpperCase()).join("\n") + "\n";
	return `data:text/plain;charset=utf-8,${encodeURIComponent(items)}`;
});

</script>

<style scoped>
a {
	display: block;
	padding: 4px;
	cursor: pointer;
	margin: 0;
	padding: 0;
}
</style>
