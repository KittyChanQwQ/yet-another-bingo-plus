import { ref, Ref } from "vue";
import { useEventListener } from "@vueuse/core";
import { DRAG_DATA_KEY } from "./useDragItem";

type DragTargetProps = {
	element: Ref<HTMLElement | undefined>,
	filter?: (data: any) => boolean,
	action?: "move" | "copy" | 'link',
};

type DropListener = (data: any, e: DragEvent) => void;

export function useDragTarget({
	element,
	filter = () => true,
	action = "move",
}: DragTargetProps) {
	const isDraggedOver = ref(false);

	const listeners: DropListener[] = [];
	const onDrop = (listener: DropListener) => listeners.push(listener);

	useEventListener(element, "dragenter", (e: DragEvent) => {
		if (!e.dataTransfer) return;
		const dataStr = e.dataTransfer.types[0];
		const data = dataStr ? JSON.parse(dataStr) : undefined;

		if (!filter(data)) return;
		isDraggedOver.value = true;
	});

	useEventListener(element, "dragleave", (e: DragEvent) => {
		isDraggedOver.value = false;
	});

	useEventListener(element, "dragover", (e: DragEvent) => {
		if (!e.dataTransfer) return;
		const dataStr = e.dataTransfer.types[0];
		const data = dataStr ? JSON.parse(dataStr) : undefined;

		if (!filter(data)) {
			e.dataTransfer.dropEffect = "none";
			return;
		}

		isDraggedOver.value = true;

		e.preventDefault();
		e.stopPropagation();
		e.dataTransfer.dropEffect = action;
	});

	useEventListener(element, "drop", (e: DragEvent) => {
		isDraggedOver.value = false;

		if (!e.dataTransfer) return;
		const dataStr = e.dataTransfer.types[0];
		const data = dataStr ? JSON.parse(dataStr) : undefined;

		if (!filter(data)) return;
		e.preventDefault();
		e.stopPropagation();

		for (const listener of listeners)
			listener(data, e);
	});

	return { isDraggedOver, onDrop };
}