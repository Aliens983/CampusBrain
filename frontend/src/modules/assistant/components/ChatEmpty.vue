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
    <div class="chat-empty__icon">AI</div>
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
  width: 58px;
  height: 58px;
  margin-bottom: 18px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  font-size: 16px;
  color: #fff;
  font-weight: 800;
  background: linear-gradient(135deg, #1167b1, #3fb6ff);
  box-shadow: 0 8px 18px rgba(30, 152, 242, .2);
}
.chat-empty h2 { margin: 0 0 8px; font-size: 22px; }
.chat-empty p { margin: 0; color: var(--text-secondary); font-size: 13px; }
.chat-empty__suggestions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 22px;
}
.chat-empty__suggestions button {
  padding: 9px 14px;
  border: 1px solid rgba(63, 182, 255, .2);
  border-radius: 10px;
  color: #2673a8;
  background: rgba(255, 255, 255, .75);
  cursor: pointer;
  transition: .2s;
}
.chat-empty__suggestions button:hover {
  border-color: #3fb6ff;
  background: #fff;
  transform: translateY(-1px);
}
</style>
