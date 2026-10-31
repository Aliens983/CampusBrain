<script setup lang="ts">
/** 空状态：引导语 + 示例问题 */
const emit = defineEmits<{ (e: 'ask', question: string): void }>()

const suggestions = [
  '可以预约哪些校园服务？',
  '仓前校区上午9-10点还能预约教师吗',
  '如何查询教室的可用时段？'
]
</script>

<template>
  <div class="chat-empty">
    <div class="chat-empty__icon">问</div>
    <p class="chat-empty__eyebrow">
      Knowledge & booking · HZNU
    </p>
    <h2>今天想了解什么？</h2>
    <p>你可以询问校园服务、预约规则，也可以直接查询可用时段。</p>
    <div class="chat-empty__suggestions">
      <button v-for="s in suggestions" :key="s" @click="emit('ask', s)">{{ s }}</button>
    </div>
  </div>
</template>

<style scoped>
.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 36px 18px;
  text-align: center;
}

.chat-empty__icon {
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  margin-bottom: 16px;
  border: 1px solid var(--lake);
  border-radius: var(--radius-md);
  background: var(--lake);
  color: #f7f1e4;
  font-family: var(--font-serif);
  font-size: 24px;
  font-weight: 600;
}

.chat-empty__eyebrow {
  margin: 0 0 10px;
  font-family: var(--font-latin);
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--gold);
}

.chat-empty h2 {
  margin: 0 0 8px;
  font-family: var(--font-serif);
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--ink);
}

.chat-empty p {
  margin: 0;
  color: var(--ink-3);
  font-size: 12.5px;
}

.chat-empty__suggestions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 24px;
}

.chat-empty__suggestions button {
  padding: 9px 14px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface);
  color: var(--ink-2);
  font-family: var(--font-sans);
  font-size: 12px;
  cursor: pointer;
  transition:
    transform 0.16s var(--ease-out),
    border-color 0.16s ease,
    background-color 0.16s ease,
    color 0.16s ease;
}

.chat-empty__suggestions button:hover {
  transform: translateY(-1px);
  border-color: rgba(176, 138, 69, 0.45);
  background: var(--surface-2);
  color: var(--lake);
}
</style>
