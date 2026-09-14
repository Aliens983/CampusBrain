import { computed, reactive, ref } from 'vue'
import type { Ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/common/utils/request'
import type { UserRole } from '@/common/types'

/** el-tag 可用类型（角色标签） */
export type RoleTagType = 'danger' | 'warning' | 'success'

/** 修改密码弹窗的响应式状态与提交逻辑（PUT /users/password） */
export function usePasswordChange(visible: Ref<boolean>) {
  const pwdSubmitting = ref(false)
  const pwdFormRef = ref<{ validate: () => Promise<unknown>; resetFields: () => void }>()
  const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
  const pwdRules = {
    oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
    newPassword: [
      { required: true, message: '请输入新密码', trigger: 'blur' },
      { min: 6, message: '新密码至少 6 位', trigger: 'blur' },
    ],
    confirmPassword: [
      { required: true, message: '请再次输入新密码', trigger: 'blur' },
      {
        validator: (_rule: unknown, value: string, callback: (e?: Error) => void) => {
          if (value !== pwdForm.newPassword) callback(new Error('两次输入的新密码不一致'))
          else callback()
        },
        trigger: 'blur',
      },
    ],
  }

  // 新密码强度：长度 + 字符构成综合评分，驱动强度条展示
  const pwdStrength = computed(() => {
    const v = pwdForm.newPassword
    if (!v) return { cells: 0, label: '', color: '' }
    let cells = 1
    if (v.length >= 8) cells++
    if (/[A-Za-z]/.test(v) && /\d/.test(v)) cells++
    if (/[^A-Za-z0-9]/.test(v)) cells++
    cells = Math.min(cells, 4)
    const map = [
      { label: '弱', color: '#ef4444' },
      { label: '一般', color: '#f97316' },
      { label: '中', color: '#eab308' },
      { label: '强', color: '#22c55e' },
    ]
    return { cells, label: map[cells - 1].label, color: map[cells - 1].color }
  })

  function resetPwdForm() {
    pwdFormRef.value?.resetFields()
    pwdSubmitting.value = false
  }

  async function submitChangePassword() {
    const valid = await pwdFormRef.value?.validate().catch(() => false)
    if (!valid) return
    pwdSubmitting.value = true
    try {
      await request.put('/users/password', {
        oldPassword: pwdForm.oldPassword,
        newPassword: pwdForm.newPassword,
      })
      ElMessage.success('密码修改成功')
      visible.value = false
    } catch (err) {
      // 业务码错误：拦截器按「由组件处理」静默 reject(Error(msg))；
      // HTTP 层错误(4xx/5xx)拦截器已弹过提示(isAxiosError=true)，这里不重复弹
      const e = err as { isAxiosError?: boolean; message?: string }
      if (!e?.isAxiosError) {
        ElMessage.error(e?.message || '密码修改失败，请稍后重试')
      }
    } finally {
      pwdSubmitting.value = false
    }
  }

  return {
    pwdSubmitting,
    pwdFormRef,
    pwdForm,
    pwdRules,
    pwdStrength,
    resetPwdForm,
    submitChangePassword,
  }
}

/** 个人通知偏好（与后端 /users/me/notify 同步） */
export function useNotifyPrefs() {
  const prefs = reactive({ emailOn: true })

  async function loadPrefs() {
    try {
      const d = await request.get('/users/me/notify') as any
      if (d && typeof d.emailOn === 'boolean') {
        prefs.emailOn = d.emailOn
      }
    } catch {
      // 加载失败用默认值
    }
  }

  async function savePrefs() {
    try {
      await request.put('/users/me/notify', { ...prefs })
    } catch {
      // 保存失败由响应拦截器统一提示
    }
  }

  return { prefs, loadPrefs, savePrefs }
}

export function roleLabelOf(role?: UserRole): string {
  if (role === 'super_admin') return '超级管理员'
  if (role === 'admin') return '管理员'
  if (role === 'teacher') return '教师'
  return '普通用户'
}

export function roleTagTypeOf(role?: UserRole): RoleTagType {
  if (role === 'super_admin' || role === 'admin') return 'danger'
  if (role === 'teacher') return 'warning'
  return 'success'
}
