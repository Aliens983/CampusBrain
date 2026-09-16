<script setup lang="ts">
import type { UploadRequestOptions } from 'element-plus'
import type { ServiceCard } from '@/common/types'

/** 服务编辑抽屉 */
const props = defineProps<{
  visible: boolean
  service: ServiceCard | null
  form: { name: string; category: string; description: string; image: string }
  saving: boolean
  assetUrl: (path?: string) => string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'upload', file: File): void
  (e: 'save'): void
}>()
</script>

<template>
  <el-drawer
    :model-value="props.visible"
    title="服务编辑"
    size="460px"
    class="svc-drawer"
    @update:model-value="emit('update:visible', $event)"
  >
    <template v-if="props.service">
      <el-form label-position="top">
        <el-form-item label="服务名称">
          <el-input v-model="props.form.name" />
        </el-form-item>
        <el-form-item label="服务分类">
          <el-input v-model="props.form.category" />
        </el-form-item>
        <el-form-item label="服务说明">
          <el-input v-model="props.form.description" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="服务封面">
          <el-upload
            :show-file-list="false"
            accept="image/*"
            :http-request="(o: UploadRequestOptions) => emit('upload', o.file as File)"
          >
            <img
              v-if="props.form.image"
              :src="props.assetUrl(props.form.image)"
              class="cover-prev"
              alt="封面"
            >
            <el-button v-else size="small">上传封面图片</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <el-button
        type="primary"
        style="width: 100%"
        :loading="props.saving"
        @click="emit('save')"
      >
        保存修改
      </el-button>
    </template>
  </el-drawer>
</template>

<style scoped lang="scss">
.cover-prev {
  display: block; width: 100%; max-height: 150px; object-fit: cover;
  border-radius: 12px; border: 1px solid var(--border-soft);
}
</style>
