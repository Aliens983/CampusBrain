<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    width="560px"
    :lock-scroll="false"
  >
    <div class="detail-dialog__body">
      <div
        v-for="item in items"
        :key="item"
        class="detail-dialog__item"
      >
        {{ item }}
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  visible: boolean
  title: string
  items: string[]
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
})
</script>

<style scoped lang="scss">
.detail-dialog__body {
  display: grid;
  gap: 10px;
}

.detail-dialog__item {
  padding: 12px;
  border-radius: 10px;
  background: linear-gradient(180deg, #fff, #F9FCFF);
  border: 1px solid var(--border-soft);
  font-size: 13px;
}
</style>
