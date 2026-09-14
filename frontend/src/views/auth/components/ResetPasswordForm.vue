<template>
  <section class="auth-card">
    <div class="auth-card__head">
      <h2>重置密码</h2>
      <p>输入邮箱获取验证码，设置新密码后登录。</p>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      size="large"
    >
      <el-form-item prop="email">
        <el-input
          v-model="form.email"
          placeholder="邮箱"
        />
      </el-form-item>
      <el-form-item prop="code">
        <div
          class="captcha-row"
          style="grid-template-columns: 1fr auto"
        >
          <el-input
            v-model="form.code"
            placeholder="邮箱验证码"
          />
          <el-button
            :loading="sendingCode"
            @click="emit('send-code')"
          >
            发送验证码
          </el-button>
        </div>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          placeholder="新密码"
        />
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          placeholder="确认新密码"
        />
      </el-form-item>

      <el-button
        type="primary"
        class="submit-btn"
        :loading="loading"
        @click="handleSubmit"
      >
        确认重置
      </el-button>

      <div class="auth-footer">
        <span />
        <el-link
          type="primary"
          @click="emit('back-login')"
        >
          返回登录
        </el-link>
      </div>
    </el-form>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { ResetFormState } from '../composables'

defineProps<{
  form: ResetFormState
  rules: FormRules
  loading: boolean
  sendingCode: boolean
}>()

const emit = defineEmits<{
  submit: []
  'send-code': []
  'back-login': []
}>()

const formRef = ref<FormInstance>()

async function handleSubmit() {
  await formRef.value?.validate()
  emit('submit')
}
</script>

<style scoped lang="scss">
.auth-card {
  padding: 40px;
  background: rgba(255, 255, 255, 0.98);
}

.auth-card__head h2 {
  margin: 0 0 8px;
  font-size: 24px;
}

.auth-card__head p {
  margin: 0 0 24px;
  color: var(--text-secondary);
}

.captcha-row {
  display: grid;
  grid-template-columns: 1fr 120px;
  gap: 10px;
  width: 100%;
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
}

.auth-footer {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
}
</style>
