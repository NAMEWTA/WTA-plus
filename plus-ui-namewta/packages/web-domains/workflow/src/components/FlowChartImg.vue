<template>
  <div
    ref="wrapper"
    class="image-wrapper"
    :style="transformStyle"
    @wheel.prevent="zoom"
    @mousedown="startDrag"
    @mousemove="drag"
    @mouseup="stopDrag"
    @mouseleave="stopDrag"
    @dblclick="reset"
  >
    <el-image :src="imgUrl" class="scalable-image" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';

withDefaults(defineProps<{ imgUrl?: string }>(), { imgUrl: '' });
const wrapper = ref<HTMLElement>();
const scale = ref(1);
const translate = ref({ x: 0, y: 0 });
let dragging = false;
let start = { x: 0, y: 0 };
const transformStyle = computed(() => ({
  transform: `translate(${translate.value.x}px, ${translate.value.y}px) scale(${scale.value})`,
  transition: dragging ? 'none' : 'transform 0.2s ease'
}));
function zoom(event: WheelEvent) {
  scale.value = Math.max(0.5, Math.min(3, scale.value - event.deltaY / 1000));
  translate.value = { x: 0, y: 0 };
}
function startDrag(event: MouseEvent) {
  if (scale.value <= 1) return;
  dragging = true;
  start = { x: event.clientX, y: event.clientY };
}
function drag(event: MouseEvent) {
  if (!dragging || !wrapper.value) return;
  const next = {
    x: translate.value.x + event.clientX - start.x,
    y: translate.value.y + event.clientY - start.y
  };
  const bounds = dragBounds();
  translate.value = {
    x: Math.max(bounds.minX, Math.min(next.x, bounds.maxX)),
    y: Math.max(bounds.minY, Math.min(next.y, bounds.maxY))
  };
  start = { x: event.clientX, y: event.clientY };
}
function dragBounds() {
  const element = wrapper.value;
  if (!element) return { minX: 0, maxX: 0, minY: 0, maxY: 0 };
  const image = element.getBoundingClientRect();
  const container = element.parentElement?.getBoundingClientRect() ?? image;
  const horizontal = Math.max(0, (image.width * scale.value - container.width) / 2);
  const vertical = Math.max(0, (image.height * scale.value - container.height) / 2);
  return { minX: -horizontal, maxX: horizontal, minY: -vertical, maxY: vertical };
}
function stopDrag() {
  dragging = false;
}
function reset() {
  scale.value = 1;
  translate.value = { x: 0, y: 0 };
}
</script>

<style scoped>
.image-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  overflow: hidden;
  cursor: grab;
  user-select: none;
}
.scalable-image {
  width: 100%;
  padding: 15px;
  object-fit: contain;
}
</style>
