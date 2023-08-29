import { ref, unref, Ref } from "vue";
import { MaybeRef, useEventListener } from "@vueuse/core";

type DragItemProps = {
	element: Ref<HTMLElement | undefined>,
	effectAllowed?: "move" | "copy" | 'link',
	data: MaybeRef<any>,
};

type DragListener = (isSuccessful: boolean) => void;

export const DRAG_DATA_KEY = "application/yamb";

export function useDragItem({
	element,
	effectAllowed = "move",
	data,
}: DragItemProps) {
	const isDragging = ref(false);

	const listeners: DragListener[] = [];
	const onDragEnd = (listener: DragListener) => listeners.push(listener);

	useEventListener(element, "mousedown", () => {
		// Clear all text selection before dragging (prevents selection boxes)
		const selection = window.getSelection();
		if (selection && "empty" in selection)
			selection.empty(); // chrome
		if (selection && "removeAllRanges" in selection)
			selection.removeAllRanges(); // firefox
	});

	useEventListener(element, "dragstart", (e: DragEvent) => {
		if (!e.dataTransfer) return;
		e.dataTransfer.setData(DRAG_DATA_KEY, JSON.stringify(unref(data)));
		e.dataTransfer.effectAllowed = effectAllowed;
		e.stopPropagation();

		isDragging.value = true;
	});

	useEventListener(element, "dragend", (e: DragEvent) => {
		isDragging.value = false;
		e.stopPropagation();

		const isSuccessful = e.dataTransfer?.dropEffect === effectAllowed;

		for (const listener of listeners)
			listener(isSuccessful);
	});

	return { isDragging, onDragEnd };
}
