<script setup lang="ts">
import type { UploadRequestOptions } from 'element-plus'
import type { ServiceCategoryOption } from '@/common/campus'

/** 新增服务抽屉 */
const props = defineProps<{
  visible: boolean
  form: {
    name: string; categoryId: number; campus: string; capacity: number
    description: string; location: string; image: string
  }
  categories: ServiceCategoryOption[]
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
    title="新增服务"
    size="480px"
    class="svc-drawer"
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form label-position="top">
      <el-form-item label="服务名称">
        <el-input v-model="props.form.name" placeholder="请输入服务名称" />
      </el-form-item>
      <el-form-item label="服务分类">
        <el-select
          v-model="props.form.categoryId"
          style="width: 100%"
          placeholder="请选择业务分类"
        >
          <el-option
            v-for="opt in props.categories"
            :key="opt.id"
            :label="opt.name"
            :value="opt.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="所属校区">
        <el-select v-model="props.form.campus" style="width: 100%">
          <el-option label="仓前校区" value="cq" />
          <el-option label="下沙校区" value="xs" />
        </el-select>
      </el-form-item>
      <el-form-item label="预约容量">
        <el-input-number
          v-model="props.form.capacity"
          :min="-1"
          :step="1"
          style="width: 100%"
        />
        <div class="field-tip">
          -1 表示不限名额；活动/教室等先到先得类建议设具体人数。
        </div>
      </el-form-item>
      <el-form-item label="服务说明">
        <el-input
          v-model="props.form.description"
          type="textarea"
          :rows="5"
          placeholder="请输入服务说明"
        />
      </el-form-item>
      <el-form-item label="开放范围">
        <el-input v-model="props.form.location" placeholder="如：全校师生" />
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
      保存
    </el-button>
  </el-drawer>
</template>

<style scoped lang="scss">
.field-tip { margin-top: 4px; font-size: 12px; line-height: 1.6; color: var(--text-tertiary); }
.cover-prev {
  display: block; width: 100%; max-height: 150px; object-fit: cover;
  border-radius: 12px; border: 1px solid var(--border-soft);
}
</style>
