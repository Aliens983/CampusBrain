<script setup lang="ts">
import { computed, ref } from 'vue'
import type { DocumentItem, UploadVisibility } from '../composables'

/** 管理员可见的知识库条 + 文档管理弹窗 */
const props = defineProps<{
  documents: DocumentItem[]
  uploading: boolean
  visible: boolean
  uploadVisibility: UploadVisibility
  statusType: (status?: string) => 'success' | 'warning' | 'danger' | 'info'
  statusLabel: (status?: string) => string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'update:uploadVisibility', value: UploadVisibility): void
  (e: 'refresh'): void
  (e: 'pick', files: FileList): void
  (e: 'delete', doc: DocumentItem): void
}>()

const visibilityModel = computed({
  get: () => props.uploadVisibility,
  set: (value: UploadVisibility) => emit('update:uploadVisibility', value),
})

// 隐藏的 file input 由本组件自己持有，父组件只需响应 pick 事件
const fileInput = ref<HTMLInputElement | null>(null)

function triggerUpload() { fileInput.value?.click() }

function onFileSelected(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files) emit('pick', target.files)
  target.value = ''
}
</script>

<template>
  <section class="knowledge-strip">
    <div class="knowledge-strip__title">
      <span class="strip-icon">KB</span>
      <strong>知识库文档</strong>
      <span class="doc-count">{{ props.documents.length }} 份</span>
    </div>
    <div class="knowledge-strip__actions">
      <input
        ref="fileInput"
        type="file"
        multiple
        accept=".md,.markdown,.pdf,.txt,.xlsx,.xls"
        hidden
        @change="onFileSelected"
      >
      <span class="knowledge-strip__hint">支持 PDF、Markdown、TXT、Excel</span>
      <el-select v-model="visibilityModel" size="small" class="visibility-select" aria-label="文档可见范围">
        <el-option label="公开" value="PUBLIC" />
        <el-option label="私有" value="PRIVATE" />
      </el-select>
      <el-button text @click="emit('update:visible', true)">文档管理</el-button>
      <el-button text @click="emit('refresh')">刷新</el-button>
      <el-button type="primary" :loading="props.uploading" @click="triggerUpload">上传文档</el-button>
    </div>

    <el-dialog
      :model-value="props.visible"
      title="知识库文档管理"
      width="720px"
      @update:model-value="emit('update:visible', $event)"
    >
      <el-table v-if="props.documents.length" :data="props.documents" size="small">
        <el-table-column prop="title" label="文档" min-width="220" />
        <el-table-column prop="fileType" label="类型" width="100" />
        <el-table-column label="可见" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.visibility === 'PRIVATE' ? 'info' : 'success'">
              {{ row.visibility === 'PRIVATE' ? '私有' : '公开' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="props.statusType(row.status)">{{ props.statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button type="danger" link @click="emit('delete', row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无知识库文档" :image-size="70" />
    </el-dialog>
  </section>
</template>

<style scoped>
.knowledge-strip {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 14px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.knowledge-strip__title,
.knowledge-strip__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.visibility-select {
  width: 92px;
}

.knowledge-strip__title strong {
  font-family: var(--font-serif);
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink);
}

.strip-icon {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: 1px solid var(--lake);
  border-radius: var(--radius-sm);
  background: var(--lake);
  color: #f7f1e4;
  font-family: var(--font-serif);
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.doc-count {
  padding: 3px 8px;
  border: 1px solid var(--line);
  border-radius: 3px;
  background: var(--surface-2);
  color: var(--gold);
  font-family: var(--font-latin);
  font-size: 10px;
  letter-spacing: 0.06em;
}

.knowledge-strip__hint {
  color: var(--ink-4);
  font-size: 11px;
}

@media (max-width: 720px) {
  .knowledge-strip {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .knowledge-strip__actions {
    width: 100%;
  }

  .knowledge-strip__hint {
    margin-right: auto;
  }
}
</style>
