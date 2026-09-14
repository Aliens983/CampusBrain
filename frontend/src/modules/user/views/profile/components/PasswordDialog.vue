<template>
  <!-- 修改密码：当前登录用户改自己的密码，PUT /users/password -->
  <el-dialog
    v-model="visible"
    title="修改密码"
    width="460px"
    append-to-body
    :lock-scroll="false"
    :close-on-click-modal="false"
    @closed="resetPwdForm"
  >
    <div class="pwd-banner">
      <div class="pwd-banner__ico">
        <el-icon :size="18"><Lock /></el-icon>
      </div>
      <div class="pwd-banner__txt">
        <b>账号安全</b>
        <span>先验证当前密码，再设置新密码</span>
      </div>
    </div>

    <el-form
      ref="pwdFormRef"
      :model="pwdForm"
      :rules="pwdRules"
      label-position="top"
    >
      <el-form-item
        label="当前密码"
        prop="oldPassword"
      >
        <el-input
          v-model="pwdForm.oldPassword"
          type="password"
          show-password
          size="large"
          placeholder="请输入当前密码"
          autocomplete="current-password"
        >
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item
        label="新密码"
        prop="newPassword"
      >
        <el-input
          v-model="pwdForm.newPassword"
          type="password"
          show-password
          size="large"
          placeholder="6 位以上新密码"
          autocomplete="new-password"
        >
          <template #prefix>
            <el-icon><Key /></el-icon>
          </template>
        </el-input>
        <div
          v-if="pwdStrength.cells"
          class="pwd-strength"
        >
          <span class="pwd-strength__cells">
            <i
              v-for="n in 4"
              :key="n"
              :style="n <= pwdStrength.cells ? { background: pwdStrength.color } : {}"
            />
          </span>
          <span
            class="pwd-strength__label"
            :style="{ color: pwdStrength.color }"
          >
            强度{{ pwdStrength.label }}
          </span>
        </div>
      </el-form-item>
      <el-form-item
        label="确认新密码"
        prop="confirmPassword"
      >
        <el-input
          v-model="pwdForm.confirmPassword"
          type="password"
          show-password
          size="large"
          placeholder="再次输入新密码"
          autocomplete="new-password"
          @keyup.enter="submitChangePassword"
        >
          <template #prefix>
            <el-icon><CircleCheck /></el-icon>
          </template>
        </el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button
        class="pwd-btn"
        @click="visible = false"
      >
        取 消
      </el-button>
      <el-button
        class="pwd-btn pwd-btn--primary"
        type="primary"
        :loading="pwdSubmitting"
        @click="submitChangePassword"
      >
        确认修改
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { CircleCheck, Key, Lock } from '@element-plus/icons-vue'
import { usePasswordChange } from '../composables'

const visible = defineModel<boolean>({ required: true })

const {
  pwdSubmitting,
  pwdFormRef,
  pwdForm,
  pwdRules,
  pwdStrength,
  resetPwdForm,
  submitChangePassword,
} = usePasswordChange(visible)
</script>

<style scoped lang="scss">
/* 修改密码弹窗美化（append-to-body 但元素均在本模板内，scoped 即可命中，不外溢） */
.pwd-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: -2px 0 18px;
  padding: 12px 14px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(14, 108, 214, 0.08), rgba(63, 182, 255, 0.14) 75%);
}
.pwd-banner__ico {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 11px;
  color: #fff;
  background: linear-gradient(135deg, #0e6cd6, #3fb6ff);
  box-shadow: 0 4px 12px rgba(62, 175, 255, 0.32);
  flex-shrink: 0;
}
.pwd-banner__txt {
  display: grid;
  gap: 2px;
}
.pwd-banner__txt b {
  font-size: 14px;
  color: var(--text-primary);
}
.pwd-banner__txt span {
  font-size: 12px;
  color: var(--text-secondary);
}

.pwd-strength {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}
.pwd-strength__cells {
  display: flex;
  flex: 1;
  gap: 6px;
}
.pwd-strength__cells i {
  flex: 1;
  height: 4px;
  border-radius: 4px;
  background: var(--border-soft);
  transition: background 0.25s ease;
}
.pwd-strength__label {
  width: 52px;
  font-size: 12px;
  text-align: right;
}

.pwd-btn {
  padding: 8px 22px;
  border-radius: 10px;
  font-weight: 500;
}
.pwd-btn--primary {
  border: none;
  background: linear-gradient(135deg, #0e6cd6, #3fb6ff);
  box-shadow: 0 6px 14px rgba(62, 175, 255, 0.35);
}
.pwd-btn--primary:hover,
.pwd-btn--primary:focus {
  border: none;
  background: linear-gradient(135deg, #0b5ec0, #2ea6f0);
}
</style>
