<template>
  <div class="page-shell">
    <section class="page-hero">
      <el-button
        text
        @click="router.back()"
      >
        ← 返回
      </el-button>
      <h1 class="page-hero__title">
        服务详情
      </h1>
    </section>
    <el-card
      v-if="service"
      class="panel-card"
    >
      <div class="service-detail">
        <div
          class="cover-badge"
          :class="service.image"
        >
          {{ service.code }}
        </div>
        <h3>{{ service.name }}</h3>
        <p class="muted">
          {{ service.description }}
        </p>
        <div class="info-list">
          <div class="info-row">
            <span>业务类别</span><strong>{{ service.category }}</strong>
          </div>
          <div class="info-row">
            <span>服务范围</span><strong>{{ service.location }}</strong>
          </div>
          <div class="info-row">
            <span>说明</span><strong>{{ service.priceLabel }}</strong>
          </div>
          <div class="info-row">
            <span>状态</span><el-tag :type="service.status === 'available' ? 'success' : 'warning'">
              {{ service.status === 'available' ? '可预约' : '维护中' }}
            </el-tag>
          </div>
        </div>
        <div class="tag-wrap">
          <el-tag
            v-for="tag in service.tags"
            :key="tag"
            round
          >
            {{ tag }}
          </el-tag>
        </div>
        <el-button
          type="primary"
          size="large"
          @click="handleBook"
        >
          立即预约
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/common/utils/request'
import { fetchServiceCards } from '@/common/campus'
import type { ServiceCard } from '@/common/types'

const router = useRouter()
const route = useRoute()
const service = ref<ServiceCard | null>(null)
const booking = ref(false)

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    const services = await fetchServiceCards()
    service.value = services.find(s => s.id === id) || null
    if (!service.value) ElMessage.warning('未找到该服务')
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取服务详情失败')
  }
})

async function handleBook() {
  if (!service.value) return
  booking.value = true
  try {
    await request.post('/app/bookings', {
      serviceIds: [service.value.id],
    })
    ElMessage.success('预约已提交成功')
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '预约提交失败')
  } finally {
    booking.value = false
  }
}
</script>

<style scoped lang="scss">
.service-detail { display: grid; gap: 16px; max-width: 600px; }
.cover-badge { width: 72px; height: 72px; border-radius: 18px; display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 700; color: #fff; }
</style>