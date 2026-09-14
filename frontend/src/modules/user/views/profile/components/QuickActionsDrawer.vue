<template>
  <el-drawer
    v-model="visible"
    title="快捷操作"
    size="420px"
    :lock-scroll="false"
  >
    <template v-if="isTeacher">
      <div class="drawer-stack">
        <el-button
          type="primary"
          @click="emit('navigate', '/teacher/review')"
        >
          待我审核
        </el-button>
        <el-button
          plain
          @click="emit('navigate', '/teacher/consultations')"
        >
          我的咨询
        </el-button>
        <el-button
          plain
          @click="emit('navigate', '/teacher')"
        >
          返回教师端工作台
        </el-button>
      </div>
    </template>
    <div
      v-else
      class="drawer-stack"
    >
      <el-button
        type="primary"
        @click="emit('navigate', '/dashboard')"
      >
        返回工作台
      </el-button>
      <el-button
        plain
        @click="emit('navigate', '/bookings')"
      >
        我的预约
      </el-button>
      <el-button
        plain
        @click="emit('navigate', '/services')"
      >
        服务中心
      </el-button>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
const visible = defineModel<boolean>({ required: true })

defineProps<{ isTeacher: boolean }>()

const emit = defineEmits<{
  navigate: [to: string]
}>()
</script>

<style scoped lang="scss">
.drawer-stack { display: grid; gap: 12px; }
</style>
