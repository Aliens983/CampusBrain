<template>
  <div class="page-shell">
    <section class="page-hero page-hero--single">
      <div class="page-hero__main">
        <span class="page-hero__chip">空间预约</span>
        <h1 class="page-hero__title">
          会议室与场地资源预约
        </h1>
        <p class="page-hero__desc">
          浏览校内可用会议室与活动场地，按容量和设施筛选，在线提交预约申请。
        </p>
      </div>
      <div class="hero-actions">
        <el-input
          v-model="keyword"
          placeholder="搜索会议室、楼宇或设施"
          clearable
        />
        <el-select v-model="status">
          <el-option
            label="全部状态"
            value=""
          />
          <el-option
            label="可预约"
            value="available"
          />
          <el-option
            label="使用中"
            value="occupied"
          />
        </el-select>
      </div>
    </section>

    <section class="room-highlight">
      <div class="room-highlight__panel">
        <span>空间视图</span>
        <strong>支持从容量、楼宇和设施维度快速筛选</strong>
        <p>支持按日期和容量筛选，查看各会议室设施详情与可用时段。</p>
      </div>
      <div class="room-highlight__metrics">
        <div @click="showAvailableRooms">
          <span>开放资源</span>
          <strong>{{ availableRooms }}</strong>
        </div>
        <div @click="showBuildings">
          <span>重点楼宇</span>
          <strong>3</strong>
        </div>
      </div>
    </section>

    <section
      v-if="loading"
      style="text-align:center;padding:80px 0;color:var(--text-secondary)"
    >
      加载会议室数据中...
    </section>
    <section
      v-else-if="!filteredRooms.length"
      style="text-align:center;padding:80px 0;color:var(--text-secondary)"
    >
      暂无匹配的会议室
    </section>
    <section
      v-else
      class="resource-grid"
    >
      <article
        v-for="room in filteredRooms"
        :key="room.id"
        class="resource-card room-card"
      >
        <div
          class="cover-badge"
          :class="room.image"
        >
          {{ room.name.slice(-3) }}
        </div>
        <div
          class="section-head"
          style="margin-bottom: 8px;"
        >
          <h3 class="section-head__title">
            {{ room.name }}
          </h3>
          <el-tag :type="room.status === 'available' ? 'success' : 'warning'">
            {{ room.status === 'available' ? '可预约' : room.status === 'occupied' ? '使用中' : '维护中' }}
          </el-tag>
        </div>
        <div class="info-list">
          <div class="info-row">
            <span>位置</span><strong>{{ room.building }} {{ room.floor }}</strong>
          </div>
          <div class="info-row">
            <span>容纳人数</span><strong>{{ room.capacity }} 人</strong>
          </div>
          <div class="info-row">
            <span>开放时间</span><strong>{{ room.openTime }}</strong>
          </div>
          <div class="info-row">
            <span>管理员</span><strong>{{ room.manager }}</strong>
          </div>
        </div>
        <div class="tag-wrap">
          <el-tag
            v-for="facility in room.facilities"
            :key="facility"
            round
          >
            {{ facility }}
          </el-tag>
        </div>
        <div class="button-row">
          <el-button
            plain
            @click="selectedRoom = room"
          >
            查看详情
          </el-button>
          <el-button
            type="primary"
            @click="openBooking(room)"
          >
            提交预约
          </el-button>
        </div>
      </article>
    </section>

    <el-dialog
      v-model="resourceVisible"
      :title="resourceDialogTitle"
      width="620px"
    >
      <div class="dialog-list">
        <div
          v-for="item in resourceDialogItems"
          :key="item.title"
          class="dialog-card"
        >
          <strong>{{ item.title }}</strong>
          <p>{{ item.desc }}</p>
        </div>
      </div>
    </el-dialog>

    <el-drawer
      v-model="roomDrawerVisible"
      title="会议室详情"
      size="420px"
    >
      <template v-if="selectedRoom">
        <div class="drawer-stack">
          <div
            class="cover-badge"
            :class="selectedRoom.image"
          >
            {{ selectedRoom.name.slice(-3) }}
          </div>
          <h3>{{ selectedRoom.name }}</h3>
          <div class="info-list">
            <div class="info-row">
              <span>位置</span><strong>{{ selectedRoom.building }} {{ selectedRoom.floor }}</strong>
            </div>
            <div class="info-row">
              <span>容纳人数</span><strong>{{ selectedRoom.capacity }} 人</strong>
            </div>
            <div class="info-row">
              <span>开放时间</span><strong>{{ selectedRoom.openTime }}</strong>
            </div>
            <div class="info-row">
              <span>管理员</span><strong>{{ selectedRoom.manager }}</strong>
            </div>
          </div>
          <div class="tag-wrap">
            <el-tag
              v-for="facility in selectedRoom.facilities"
              :key="facility"
              round
            >
              {{ facility }}
            </el-tag>
          </div>
        </div>
      </template>
    </el-drawer>

    <el-dialog
      v-model="visible"
      title="会议室预约申请"
      width="620px"
    >
      <el-form
        :model="draft"
        label-position="top"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="会议室">
              <el-input
                v-model="draft.targetName"
                readonly
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预约日期">
              <el-date-picker
                v-model="draft.date"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始时间">
              <el-time-select
                v-model="draft.startTime"
                start="08:00"
                step="00:30"
                end="21:00"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间">
              <el-time-select
                v-model="draft.endTime"
                start="08:30"
                step="00:30"
                end="21:30"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="联系人">
          <el-input v-model="draft.contactName" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="draft.contactPhone" />
        </el-form-item>
        <el-form-item label="使用事由">
          <el-input
            v-model="draft.purpose"
            type="textarea"
            :rows="4"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="submit"
        >
          提交申请
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/common/utils/request'
import { roomAPI } from '@/services/api'
import type { BookingDraft, RoomResource } from '@/types'

const rooms = ref<RoomResource[]>([])
const loading = ref(true)
const keyword = ref('')
const status = ref('')
const visible = ref(false)
const resourceVisible = ref(false)
const resourceDialogTitle = ref('')
const resourceDialogItems = ref<{ title: string; desc: string }[]>([])
const selectedRoom = ref<RoomResource | null>(null)
const submitting = ref(false)
const roomDrawerVisible = computed({
  get: () => Boolean(selectedRoom.value),
  set: (value: boolean) => {
    if (!value) selectedRoom.value = null
  },
})
const draft = reactive<BookingDraft>({
  targetId: 0,
  targetName: '',
  date: '',
  startTime: '09:00',
  endTime: '10:00',
  contactName: '陈晓宇',
  contactPhone: '13800138000',
  purpose: '',
  notes: '',
})

onMounted(async () => {
  try {
    const data = await roomAPI.getRooms({}) as any
    if (Array.isArray(data)) {
      rooms.value = data
    } else if (data?.records && Array.isArray(data.records)) {
      rooms.value = data.records
    } else if (data?.data && Array.isArray(data.data)) {
      rooms.value = data.data
    }
  } catch {
    ElMessage.warning('获取会议室列表失败')
  } finally {
    loading.value = false
  }
})

const filteredRooms = computed(() =>
  rooms.value.filter((room) => {
    const searchTarget = `${room.name}|${room.building}|${room.facilities.join('|')}`
    const matchKeyword = !keyword.value || searchTarget.toLowerCase().includes(keyword.value.toLowerCase())
    const matchStatus = !status.value || room.status === status.value
    return matchKeyword && matchStatus
  }),
)
const availableRooms = computed(() => rooms.value.filter((room) => room.status === 'available').length)

function showAvailableRooms() {
  resourceDialogTitle.value = '当前可预约会议室'
  resourceDialogItems.value = rooms.value
    .filter((room) => room.status === 'available')
    .map((room) => ({ title: room.name, desc: `${room.building} ${room.floor} / ${room.capacity} 人 / ${room.openTime}` }))
  resourceVisible.value = true
}

function showBuildings() {
  resourceDialogTitle.value = '重点楼宇资源'
  resourceDialogItems.value = [
    { title: '创新中心', desc: '适合项目评审、路演和中型讨论。' },
    { title: '行政楼', desc: '适合正式会议、接待与综合活动。' },
    { title: '图书馆', desc: '适合研讨、小组学习和静态讨论。' },
  ]
  resourceVisible.value = true
}

async function openBooking(room: RoomResource) {
  draft.targetId = room.id
  draft.targetName = room.name
  draft.purpose = ''
  visible.value = true
}

async function submit() {
  if (!draft.date || !draft.startTime || !draft.endTime) {
    ElMessage.warning('请填写完整的预约时间')
    return
  }
  submitting.value = true
  try {
    await request.post('/app/bookings/room', {
      roomId: String(draft.targetId),
      date: draft.date,
      startTime: draft.startTime,
      endTime: draft.endTime,
      purpose: draft.purpose || '会议室预约',
      remarks: draft.notes || '',
    })
    visible.value = false
    ElMessage.success('会议室预约已提交成功')
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '预约提交失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.hero-actions { display: grid; gap: 12px; width: min(320px, 100%); }
.room-highlight { display: grid; grid-template-columns: 1.2fr .8fr; gap: 16px; }
.room-highlight__panel, .room-highlight__metrics { border-radius: 24px; border: 1px solid var(--border-soft); background: rgba(255,255,255,.92); box-shadow: var(--shadow-card); }
.room-highlight__panel { display: grid; gap: 10px; padding: 24px; }
.room-highlight__panel span, .room-highlight__panel p, .room-highlight__metrics span { color: var(--text-secondary); }
.room-highlight__panel strong { font-size: 24px; }
.room-highlight__panel p { margin: 0; line-height: 1.7; }
.room-highlight__metrics { display: grid; grid-template-columns: repeat(2,1fr); gap: 1px; overflow: hidden; }
.room-highlight__metrics div { display: grid; gap: 8px; padding: 24px; background: linear-gradient(180deg, #fff, #f8fbff); cursor: pointer; transition: transform .24s ease; }
.room-highlight__metrics div:hover { transform: translateY(-4px); }
.room-highlight__metrics strong { font-size: 34px; }
.room-card { isolation: isolate; }
.room-card::after { background: linear-gradient(135deg, rgba(20,88,212,.1), transparent 45%, rgba(255,255,255,.4)); }
.tag-wrap { display: flex; flex-wrap: wrap; gap: 8px; }
.button-row { display: flex; gap: 10px; }
.dialog-list, .drawer-stack { display: grid; gap: 12px; }
.dialog-card { padding: 16px; border-radius: 18px; background: linear-gradient(180deg, #fff, #f8fbff); border: 1px solid var(--border-soft); }
.dialog-card p { margin: 6px 0; color: var(--text-secondary); }
@media (max-width: 960px) { .room-highlight { grid-template-columns: 1fr; } .button-row { flex-direction: column; } }
</style>