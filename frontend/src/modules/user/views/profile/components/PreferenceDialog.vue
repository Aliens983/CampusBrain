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
    <div style="display:grid;gap:12px;line-height:1.6">
      <div
        class="notify-pref"
        style="display:flex;align-items:center;justify-content:space-between;gap:16px;padding:14px 16px;border:1px solid var(--border-soft);border-radius:14px"
      >
        <div>
          <strong>邮件通知</strong>
          <p style="margin:4px 0 0;color:var(--text-secondary);font-size:13px">
            审核通过/拒绝结果邮件；需管理员全局邮件策略也开启才生效
          </p>
        </div>
        <el-switch
          v-model="emailOn"
          @change="emit('save')"
        />
      </div>
      <p style="margin:2px 0 0;color:var(--text-secondary);font-size:12px">
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
