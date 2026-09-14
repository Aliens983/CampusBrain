<template>
  <div class="page-shell">
    <section class="page-hero detail-page-hero">
      <button
        class="back-btn"
        @click="router.back()"
      >
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </button>
      <h1 class="page-hero__title">
        服务详情
      </h1>
    </section>

    <el-card
      v-if="service"
      class="panel-card"
    >
      <div class="service-detail">
        <ServiceHeader :service="service" />

        <!-- 模式一：教师咨询 —— 选咨询师 + 选时段（已实现） -->
        <ConsultationPanel
          v-if="consultants.length"
          v-model:booking-slot-id="bookingSlotId"
          :consultants="consultants"
          :selected="selected"
          :slots="slots"
          :loading-slots="loadingSlots"
          :submitting="submitting"
          @select="selectConsultant"
          @chat="startConsultChat"
          @submit="submitConsultation"
        />

        <!-- 模式二：设备借用 —— 选设备 + 数量 + 日期/起止时段（到点自动归还） -->
        <EquipmentPanel
          v-else-if="equipmentMode"
          v-model:qty="borrowQty"
          v-model:date="borrowDate"
          v-model:start="borrowStart"
          v-model:end="borrowEnd"
          :equipment="equipment"
          :selected-equipment="selectedEquipment"
          :submitting="submitting"
          @select="selectEquipment"
          @submit="submitBorrow"
        />

        <!-- 模式二·教室：选教室 + 自选时段（一间教室一时段仅一人） -->
        <RoomPanel
          v-else-if="roomMode"
          v-model:date="borrowDate"
          v-model:start="borrowStart"
          v-model:end="borrowEnd"
          :rooms="rooms"
          :selected-room="selectedRoom"
          :submitting="submitting"
          @select="selectRoom"
          @submit="submitRoom"
        />

        <!-- 模式三：普通服务 —— 通用预约 -->
        <el-button
          v-else
          type="primary"
          size="large"
          :loading="booking"
          @click="handleBook"
        >
          立即预约
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import ServiceHeader from './components/ServiceHeader.vue'
import ConsultationPanel from './components/ConsultationPanel.vue'
import EquipmentPanel from './components/EquipmentPanel.vue'
import RoomPanel from './components/RoomPanel.vue'
import { useServiceDetail } from './composables'

const router = useRouter()
const {
  service,
  booking,
  consultants,
  selected,
  slots,
  bookingSlotId,
  loadingSlots,
  equipmentMode,
  equipment,
  selectedEquipment,
  borrowQty,
  roomMode,
  rooms,
  selectedRoom,
  borrowDate,
  borrowStart,
  borrowEnd,
  submitting,
  selectConsultant,
  startConsultChat,
  submitConsultation,
  selectEquipment,
  submitBorrow,
  selectRoom,
  submitRoom,
  handleBook,
} = useServiceDetail()
</script>

<style scoped lang="scss">
.detail-page-hero {
  display: flex;
  align-items: center;
  gap: 26px;
  padding: 26px 36px;
}
.detail-page-hero .back-btn { margin-right: 6px; }
/* 让返回按钮/标题浮在 hero 装饰光效之上，避免看起来“叠字” */
.detail-page-hero > * { position: relative; z-index: 1; }
.detail-page-hero .back-btn { flex: 0 0 auto; }
.detail-page-hero .page-hero__title { margin: 0; font-size: 26px; line-height: 1.2; }
.service-detail { display: grid; gap: 18px; max-width: 760px; }
</style>
