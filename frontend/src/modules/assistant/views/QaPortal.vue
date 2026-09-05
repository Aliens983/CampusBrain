<template>
  <div class="qa-portal">
    <!-- 知识库文档 -->
    <el-card
      shadow="never"
      class="section-card"
    >
      <template #header>
        <div class="card-header">
          <span>📄 知识库文档</span>
          <el-button
            size="small"
            @click="refreshDocuments"
          >
            刷新
          </el-button>
        </div>
      </template>

      <div
        v-if="isAdmin"
        class="upload-row"
      >
        <input
          ref="fileInput"
          type="file"
          multiple
          accept=".md,.markdown,.pdf,.txt,.xlsx,.xls"
          style="display: none"
          @change="onFileSelected"
        >
        <el-button
          size="small"
          type="primary"
          :loading="uploading"
          @click="triggerUpload"
        >
          上传文档
        </el-button>
        <span class="upload-tip">支持 PDF / Markdown / TXT / Excel（.pdf .md .txt .xlsx .xls，仅管理员可上传）</span>
      </div>

      <el-table
        v-if="documents.length"
        :data="documents"
        size="small"
        class="doc-table"
      >
        <el-table-column
          prop="title"
          label="文档"
          min-width="180"
        />
        <el-table-column
          label="类型"
          width="90"
        >
          <template #default="{ row }">
            {{ row.fileType || '—' }}
          </template>
        </el-table-column>
        <el-table-column
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="statusType(row.status)"
            >
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isAdmin"
          label="操作"
          width="80"
        >
          <template #default="{ row }">
            <el-button
              size="small"
              type="danger"
              link
              @click="deleteDocument(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty
        v-else
        description="暂无文档，上传后即可基于文档问答"
        :image-size="60"
      />
    </el-card>

    <!-- AI 问答 -->
    <el-card
      shadow="never"
      class="section-card"
    >
      <template #header>
        <span>🤖 AI 助手</span>
      </template>

      <div class="qa-input">
        <el-input
          v-model="query"
          size="large"
          placeholder="问我预约相关的问题，例如：可以预约哪些服务？"
          :disabled="streaming"
          clearable
          @keyup.enter="askQuestion"
        />
        <el-button
          type="primary"
          size="large"
          class="qa-input__btn"
          :loading="streaming"
          @click="askQuestion"
        >
          {{ streaming ? '生成中…' : '提问' }}
        </el-button>
      </div>

      <div
        v-if="!answer && !streaming"
        class="example-row"
      >
        <el-tag
          v-for="q in exampleQuestions"
          :key="q"
          class="example-tag"
          effect="plain"
          @click="query = q; askQuestion()"
        >
          {{ q }}
        </el-tag>
      </div>

      <div
        v-if="streaming"
        class="streaming-hint"
      >
        AI 正在检索知识库并生成答案…
      </div>

      <div
        v-if="answer"
        class="answer-block"
      >
        <div class="answer-text">
          {{ answer }}
        </div>
        <div class="feedback-row">
          <el-button
            size="small"
            @click="recordFeedback('like')"
          >
            👍 有帮助
          </el-button>
          <el-button
            size="small"
            @click="recordFeedback('dislike')"
          >
            👎 没帮助
          </el-button>
        </div>
      </div>

      <el-empty
        v-if="!answer && !streaming"
        description="输入问题，AI 将基于文档与实时预约数据回答"
        :image-size="80"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/common/stores/user'

interface DocumentItem {
  id: number
  title: string
  fileType?: string
  fileSize?: number
  status?: string
  chunkCount?: number
}

const userStore = useUserStore()
// 仅管理员可上传/删除知识库文档
const isAdmin = computed(() =>
  ['admin', 'super_admin'].includes(userStore.userInfo?.role || ''),
)
const query = ref('')
const answer = ref('')
const streaming = ref(false)
const documents = ref<DocumentItem[]>([])
const lastMessageId = ref<number | null>(null)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const exampleQuestions = ['可以预约哪些服务？', '有哪些会议室可以预约？', '怎么预约设备？']

const BASE = '/api/v1/kb'

function authHeaders(): Record<string, string> {
  return { Authorization: `Bearer ${userStore.token}` }
}

async function refreshDocuments() {
  try {
    const resp = await fetch(`${BASE}/documents`, { headers: authHeaders() })
    const result = await resp.json()
    if (result.code === 0 || result.code === 200) {
      documents.value = result.data || []
    }
  } catch (e) {
    console.error('获取文档失败', e)
  }
}

function triggerUpload() {
  fileInput.value?.click()
}

function onFileSelected(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files) {
    for (const file of Array.from(target.files)) uploadFile(file)
  }
  target.value = ''
}

async function uploadFile(file: File) {
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const resp = await fetch(`${BASE}/documents/upload`, {
      method: 'POST',
      headers: authHeaders(),
      body: formData,
    })
    const result = await resp.json()
    if (result.code === 0 || result.code === 200) {
      ElMessage.success('上传成功')
      setTimeout(refreshDocuments, 800)
    } else {
      ElMessage.error(result.message || '上传失败')
    }
  } catch {
    ElMessage.error('上传失败，请重试')
  } finally {
    uploading.value = false
  }
}

async function deleteDocument(doc: DocumentItem) {
  try {
    const resp = await fetch(`${BASE}/documents/${doc.id}`, {
      method: 'DELETE',
      headers: authHeaders(),
    })
    const result = await resp.json()
    if (result.code === 0 || result.code === 200) {
      ElMessage.success('已删除')
      refreshDocuments()
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch {
    ElMessage.error('删除失败，请重试')
  }
}

function getSessionId(): string {
  // 每次提问用新会话，避免历史问题串入导致 LLM 重复回答/答错
  return crypto.randomUUID ? crypto.randomUUID() : String(Date.now())
}

function askQuestion() {
  const q = query.value.trim()
  if (!q || streaming.value) return

  streaming.value = true
  answer.value = ''
  lastMessageId.value = null

  const params = new URLSearchParams({
    query: q,
    sessionId: getSessionId(),
    token: userStore.token,
  })

  const es = new EventSource(`${BASE}/qa/ask/stream?${params.toString()}`)

  // 后端在回答完成后推送 messageId（用于点赞/点踩反馈）
  es.addEventListener('messageId', (event) => {
    const id = Number((event as MessageEvent).data)
    if (id) lastMessageId.value = id
  })

  es.onmessage = (event) => {
    if (event.data === '[DONE]') {
      streaming.value = false
      es.close()
      return
    }
    if (event.data.startsWith('[ERROR]')) {
      answer.value += `\n\n${event.data.replace('[ERROR] ', '')}`
      streaming.value = false
      es.close()
      return
    }
    answer.value += event.data
  }

  es.onerror = () => {
    if (!answer.value) answer.value = '连接失败，请确认后端服务已启动。'
    streaming.value = false
    es.close()
  }
}

async function recordFeedback(type: 'like' | 'dislike') {
  if (!lastMessageId.value) {
    ElMessage.warning('请先发起一次问答再反馈')
    return
  }
  try {
    const resp = await fetch(`${BASE}/qa/feedback`, {
      method: 'POST',
      headers: { ...authHeaders(), 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: getSessionId(),
        messageId: lastMessageId.value,
        feedback: type,
      }),
    })
    const result = await resp.json()
    if (result.code === 0 || result.code === 200) {
      ElMessage.success(type === 'like' ? '感谢你的点赞 👍' : '已记录，我们会继续改进 🙏')
    } else {
      ElMessage.error(result.message || '反馈提交失败')
    }
  } catch {
    ElMessage.error('反馈提交失败，请重试')
  }
}

function statusType(status?: string): 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    READY: 'success',
    UPLOADED: 'info',
    PARSING: 'warning',
    CHUNKING: 'warning',
    EMBEDDING: 'warning',
    FAILED: 'danger',
  }
  return map[status || ''] || 'info'
}

function statusLabel(status?: string): string {
  const map: Record<string, string> = {
    READY: '就绪',
    UPLOADED: '已上传',
    PARSING: '解析中',
    CHUNKING: '分块中',
    EMBEDDING: '向量化中',
    FAILED: '失败',
  }
  return map[status || ''] || status || '未知'
}

onMounted(refreshDocuments)
</script>

<style scoped>
.qa-portal {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.section-card {
  border-radius: 10px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.upload-tip {
  font-size: 12px;
  color: #999;
}
.doc-table {
  width: 100%;
}
.example-row {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.example-tag {
  cursor: pointer;
}
.qa-input {
  display: flex;
  gap: 12px;
}
.qa-input .el-input {
  flex: 1;
}
.qa-input__btn {
  flex-shrink: 0;
  min-width: 128px;
  margin: 0;
  font-weight: 600;
}
.streaming-hint {
  margin-top: 12px;
  padding: 10px 16px;
  background: #e8f4fd;
  border-radius: 8px;
  color: #0052d9;
  font-size: 13px;
}
.answer-block {
  margin-top: 16px;
}
.answer-text {
  white-space: pre-wrap;
  line-height: 1.8;
  font-size: 15px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  min-height: 60px;
}
.feedback-row {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}
</style>
