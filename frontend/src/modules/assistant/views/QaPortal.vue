<script setup lang="ts">
import type { DocumentItem } from '../composables'
import { useQaPortal } from '../composables'
import KnowledgeStrip from '../components/KnowledgeStrip.vue'
import ChatHeader from '../components/ChatHeader.vue'
import ContextStrip from '../components/ContextStrip.vue'
import ChatMessage from '../components/ChatMessage.vue'
import ChatEmpty from '../components/ChatEmpty.vue'
import ChatComposer from '../components/ChatComposer.vue'

/**
 * AI 助手页（知识库 RAG + 预约代办）
 * <p>
 * 本文件只负责组装子组件与布局；SSE、会话持久化、文档管理等逻辑全部收敛在
 * {@link useQaPortal} 中，避免单文件既管布局又管协议（原为 401 行的大文件）。
 * 该页面同时被 /assistant（用户端）与 /admin/assistant（管理端）复用，
 * 管理员额外的知识库管理能力通过 isAdmin 决定是否渲染。
 */
const {
  isAdmin, query, messages, streaming, slotChips, pendingIndex,
  sessions, currentSessionId, documents, documentsVisible, uploading,
  chatBodyRef, canFeedback,
  onSwitchSession, startNewSession, sessionLabel, formatSessionDate, resetContext,
  askQuestion, reply, onBodyScroll, recordFeedback,
  refreshDocuments, deleteDocument, statusType, statusLabel, uploadFile
} = useQaPortal()

function onDeleteDocument(doc: DocumentItem) {
  void deleteDocument(doc)
}

/** 选择文件后逐个上传 */
function onPickFiles(files: FileList) {
  for (const file of Array.from(files)) void uploadFile(file)
}
</script>

<template>
  <div class="qa-portal">
    <KnowledgeStrip
      v-if="isAdmin"
      v-model:visible="documentsVisible"
      :documents="documents"
      :uploading="uploading"
      :status-type="statusType"
      :status-label="statusLabel"
      @refresh="refreshDocuments"
      @pick="onPickFiles"
      @delete="onDeleteDocument"
    />

    <section class="chat-workspace">
      <ChatHeader
        :sessions="sessions"
        :current-session-id="currentSessionId"
        :session-label="sessionLabel"
        :format-session-date="formatSessionDate"
        @switch="onSwitchSession"
        @new="startNewSession"
      />

      <ContextStrip :chips="slotChips" @reset="resetContext" />

      <div ref="chatBodyRef" class="chat-body" @scroll="onBodyScroll">
        <template v-if="messages.length">
          <ChatMessage
            v-for="(m, idx) in messages"
            :key="idx"
            :message="m"
            :streaming="streaming && idx === messages.length - 1"
            :active="pendingIndex === idx"
            @reply="reply"
          />
        </template>
        <ChatEmpty v-else @ask="query = $event" />
      </div>

      <ChatComposer
        v-model="query"
        :streaming="streaming"
        :can-feedback="canFeedback"
        @send="askQuestion"
        @feedback="recordFeedback"
      />
    </section>
  </div>
</template>

<style scoped>
.qa-portal {
  height: calc(100vh - 126px);
  min-height: 560px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.chat-workspace {
  min-height: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(37, 99, 155, .1);
  border-radius: 14px;
  background: rgba(255, 255, 255, .9);
  box-shadow: 0 18px 50px rgba(30, 75, 115, .1);
}
.chat-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
  padding: 28px clamp(18px, 7vw, 110px);
  background: linear-gradient(180deg, rgba(249, 252, 255, .78), rgba(242, 248, 255, .5));
}
@media (max-width: 720px) {
  .qa-portal { height: calc(100vh - 156px); min-height: 520px; }
  .chat-body { padding: 20px 14px; }
}
</style>
