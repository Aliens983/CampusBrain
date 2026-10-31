<template>
  <!-- 通知偏好：点击 action-card 会打开本弹窗 -->
  <el-dialog
    v-model="visible"
    title="通知偏好"
    width="520px"
    append-to-body
    :lock-scroll="false"
    :close-on-click-modal="false"
  >
    <div class="pref-stack">
      <div class="pref-row">
        <div class="pref-row__main">
          <span class="pref-row__mark">邮</span>
          <div>
            <strong>邮件通知</strong>
            <p>审核通过 / 拒绝结果邮件；需管理员全局邮件策略也开启才生效。</p>
          </div>
        </div>
        <el-switch
          v-model="emailOn"
          @change="emit('save')"
        />
      </div>
      <p class="pref-tip">
        你的邮件偏好与管理员全局邮件策略都开启时，预约审核结果才会通过邮件通知你；任一关闭即不再发送。
      </p>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
const visible = defineModel<boolean>({ required: true })
const emailOn = defineModel<boolean>('emailOn', { required: true })

const emit = defineEmits<{
  save: []
}>()
</script>

<style scoped lang="scss">
.pref-stack {
  display: grid;
  gap: 12px;
  line-height: 1.6;
}

.pref-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-2);
}

.pref-row__main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.pref-row__mark {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border: 1px solid var(--line-strong);
  border-radius: var(--radius-sm);
  font-family: var(--font-serif);
  font-size: 13px;
  color: var(--lake);
}

.pref-row strong {
  font-size: 13px;
  color: var(--ink);
}

.pref-row p {
  margin: 4px 0 0;
  font-size: 11.5px;
  color: var(--ink-3);
}

.pref-tip {
  margin: 2px 0 0;
  font-size: 11px;
  line-height: 1.8;
  color: var(--ink-4);
}
</style>
