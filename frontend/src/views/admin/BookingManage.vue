<template>
  <div class="page-shell">
    <section class="admin-hero">
      <div class="admin-hero__main">
        <h1>预约审核</h1>
      </div>
      <div class="admin-hero__signal">
        <div class="signal-card">
          <span>全部申请</span><strong>{{ bookings.length }}</strong><small>统一接入各类预约业务</small>
        </div>
        <div class="signal-card">
          <span>待审核</span><strong>{{ pendingCount }}</strong><small>建议优先处理</small>
        </div>
      </div>
    </section>

    <el-card class="panel-card">
      <template #header>
        <div class="section-head">
          <h3 class="section-head__title">
            审核队列
          </h3>
          <el-segmented
            v-model="filter"
            :options="filters"
          />
        </div>
      </template>

      <div class="booking-stack">
        <article
          v-for="item in pagedBookings"
          :key="item.id"
          class="booking-item"
          @click="openBookingDrawer(item)"
        >
          <div class="booking-item__main">
            <div class="booking-item__head">
              <div>
                <strong>{{
                  item.equipmentName
                    ? item.serviceName + '（' + item.equipmentName + ' × ' + (item.quantity || 1) + '）'
                    : item.roomName
                      ? item.serviceName + '（' + item.roomName + '）'
                      : item.consultantName
                        ? item.serviceName + '（' + item.consultantName + '）'
                        : item.serviceName
                }}</strong>
                <p>{{ item.bookingNo }} / {{ item.applicant }} / {{ item.department }}</p>
              </div>
              <div class="head-tags">
                <el-tag
                  v-if="item.campus"
                  size="small"
                  type="info"
                  effect="plain"
                >{{ campusName(item.campus) }}</el-tag>
                <el-tag :type="statusTag(item.status)">
                  {{ statusText(item.status) }}
                </el-tag>
              </div>
            </div>
            <div class="booking-item__meta">
              <span>{{ item.location }}</span>
              <span>{{ item.date }}</span>
              <span>{{ item.timeRange }}</span>
            </div>
          </div>
          <div class="booking-item__action">
            <el-button
              size="small"
              type="success"
              :disabled="item.status !== 'pending'"
              @click.stop="handleAudit('通过', item)"
            >
              通过
            </el-button>
            <el-button
              size="small"
              type="danger"
              :disabled="item.status !== 'pending'"
              @click.stop="handleAudit('驳回', item)"
            >
              驳回
            </el-button>
          </div>
        </article>

        <el-empty
          v-if="!loading && pagedBookings.length === 0"
          description="没有符合条件的预约申请"
        />
      </div>

      <el-pagination
        class="list-pagination"
        background
        :current-page="pageNo"
        :page-size="pageSize"
        :page-sizes="[5, 10, 20]"
        :total="filteredBookings.length"
        layout="total, sizes, prev, pager, next"
        @current-change="onPageChange"
        @size-change="onPageSizeChange"
      />
    </el-card>

    <el-dialog
      v-if="overviewVisible"
      :model-value="true"
      :title="overviewTitle"
      width="560px"
      @close="overviewVisible = false"
    >
      <div class="dialog-list">
        <div
          v-for="item in overviewItems"
          :key="item"
          class="dialog-card"
        >
          {{ item }}
        </div>
      </div>
    </el-dialog>

    <el-dialog
      v-if="auditDialogVisible"
      :model-value="true"
      :title="auditDialogTitle"
      width="460px"
      @close="closeAuditDialog"
    >
      <el-input
        v-model="auditReason"
        type="textarea"
        :rows="4"
        :placeholder="auditAction === '驳回' ? '请输入驳回原因（必填）' : '请输入审核意见（选填）'"
      />
      <template #footer>
        <el-button @click="closeAuditDialog">
          取消
        </el-button>
        <el-button
          :type="auditAction === '通过' ? 'success' : 'danger'"
          :loading="auditing"
          @click="confirmAudit"
        >
          确认{{ auditAction }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-if="bookingDrawerVisible"
      :model-value="true"
      title="审核详情"
      size="460px"
      @close="closeBookingDrawer"
    >
      <template v-if="selectedBooking">
        <div class="drawer-stack">
          <div class="info-list">
            <div class="info-row">
              <span>编号</span><strong>{{ selectedBooking.bookingNo }}</strong>
            </div>
            <div
              v-if="selectedBooking.campus"
              class="info-row"
            >
              <span>校区</span><strong>{{ campusName(selectedBooking.campus) }}</strong>
            </div>
            <div class="info-row">
              <span>申请人</span><strong>{{ selectedBooking.applicant }}</strong>
            </div>
            <div
              v-if="selectedBooking.consultantName"
              class="info-row"
            >
              <span>咨询师</span><strong>{{ selectedBooking.consultantName }}</strong>
            </div>
            <div
              v-if="selectedBooking.equipmentName"
              class="info-row"
            >
              <span>借用设备</span><strong>{{ selectedBooking.equipmentName }} × {{ selectedBooking.quantity || 1 }}</strong>
            </div>
            <div
              v-if="selectedBooking.roomName"
              class="info-row"
            >
              <span>教室</span><strong>{{ selectedBooking.roomName }}</strong>
            </div>
            <div class="info-row">
              <span>部门</span><strong>{{ selectedBooking.department }}</strong>
            </div>
            <div class="info-row">
              <span>预约时间</span><strong>{{ selectedBooking.date }} {{ selectedBooking.timeRange }}</strong>
            </div>
            <div class="info-row">
              <span>用途说明</span><strong>{{ selectedBooking.remarks || '暂无' }}</strong>
            </div>
          </div>
          <el-input
            v-model="auditReason"
            type="textarea"
            :rows="4"
            :placeholder="'请输入审核意见（驳回时必填）'"
          />
          <div class="button-row">
            <el-button
              type="success"
              :loading="auditing"
              @click="handleAudit('通过')"
            >
              通过
            </el-button>
            <el-button
              type="danger"
              :loading="auditing"
              @click="handleAudit('驳回')"
            >
              驳回
            </el-button>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/common/utils/request'
import type { BookingStatus } from '@/types'

interface AdminBooking {
  orderId: number
  userId: number
  username: string
  serviceName: string
  serviceDescribe: string
  createTime: string
  updateTime: string
  manageStatus: number
  statusDescription?: string
  reason?: string
  consultantName?: string
  equipmentName?: string
  quantity?: number
  roomName?: string
  campus?: string
  slotDate?: string
  startTime?: string
  endTime?: string
}

interface BookingItem {
  id: number
  bookingNo: string
  serviceName: string
  applicant: string
  department: string
  location: string
  date: string
  timeRange: string
  status: BookingStatus
  createdAt: string
  remarks?: string
  consultantName?: string
  equipmentName?: string
  quantity?: number
  roomName?: string
  campus?: string
}

const filter = ref('all')
const overviewVisible = ref(false)
const overviewTitle = ref('')
const overviewItems = ref<string[]>([])
const selectedBooking = ref<BookingItem | null>(null)
const bookings = ref<BookingItem[]>([])
const loading = ref(false)
const auditing = ref(false)
const auditDialogVisible = ref(false)
const auditDialogTitle = ref('')
const auditAction = ref<'通过' | '驳回'>('通过')
const auditReason = ref('')
const auditTarget = ref<BookingItem | null>(null)

const filters = [
  { label: '全部', value: 'all' },
  { label: '待审核', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已完成', value: 'completed' },
  { label: '已驳回', value: 'rejected' },
]

function mapAdminBooking(item: AdminBooking): BookingItem {
  const statusMap: Record<number, BookingStatus> = { 0: 'pending', 1: 'approved', 2: 'rejected', 3: 'cancelled', 4: 'completed' }
  const dateTime = String(item.createTime || '').replace('T', ' ')
  // 咨询时段 / 设备借用：日期时段以用户选定为准
  const hasWindow = Boolean(item.consultantName || item.equipmentName || item.roomName)
  return {
    id: item.orderId,
    bookingNo: `BOOK-${String(item.orderId).padStart(6, '0')}`,
    serviceName: item.serviceName || '未命名服务',
    consultantName: item.consultantName,
    equipmentName: item.equipmentName,
    quantity: item.quantity,
    roomName: item.roomName,
    campus: item.campus,
    applicant: item.username || '未知用户',
    department: '校园统一预约中心',
    location: item.serviceDescribe || '',
    date: hasWindow ? String(item.slotDate || '').slice(0, 10) || '待定' : dateTime.slice(0, 10) || '待定',
    timeRange: hasWindow
      ? [item.startTime, item.endTime].filter(Boolean).join(' - ') || '待分配时段'
      : dateTime.slice(11, 16) || '待分配时段',
    status: statusMap[item.manageStatus] || 'pending',
    createdAt: dateTime || '待定',
    remarks: item.reason || item.statusDescription || '',
  }
}

async function loadBookings() {
  loading.value = true
  try {
    const data = await request.get('/admin/bookings') as { records?: AdminBooking[] } | AdminBooking[]
    const list = Array.isArray(data) ? data : data?.records
    if (!Array.isArray(list)) {
      throw new Error('获取预约记录失败')
    }
    bookings.value = list.map(mapAdminBooking)
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '获取预约记录失败')
  } finally {
    loading.value = false
  }
}

const filteredBookings = computed(() =>
  filter.value === 'all' ? bookings.value : bookings.value.filter((item) => item.status === filter.value)
)

// 列表分页：默认每页 5 条，可切换 5 / 10 / 20
const pageNo = ref(1)
const pageSize = ref(5)
const pagedBookings = computed(() => {
  const start = (pageNo.value - 1) * pageSize.value
  return filteredBookings.value.slice(start, start + pageSize.value)
})
function onPageChange(page: number) {
  pageNo.value = page
}
function onPageSizeChange(size: number) {
  pageSize.value = size
  pageNo.value = 1
}
// 切换状态筛选后结果变少，回到第一页避免停留在空页
watch(filter, () => {
  pageNo.value = 1
})
// 审核后当前页可能为空，自动回退到有效末页
watch(
  () => filteredBookings.value.length,
  (total) => {
    const maxPage = Math.max(1, Math.ceil(total / pageSize.value))
    if (pageNo.value > maxPage) pageNo.value = maxPage
  },
)
const pendingCount = computed(() => bookings.value.filter((item) => item.status === 'pending').length)
const bookingDrawerVisible = ref(false)

function openBookingDrawer(item: BookingItem) {
  selectedBooking.value = item
  bookingDrawerVisible.value = true
}

function closeBookingDrawer() {
  bookingDrawerVisible.value = false
  selectedBooking.value = null
  auditReason.value = ''
}

function closeAuditDialog() {
  auditDialogVisible.value = false
  auditReason.value = ''
  auditTarget.value = null
}

onMounted(() => {
  void loadBookings()
})

function handleAudit(action: '通过' | '驳回', item?: BookingItem) {
  const target = item || selectedBooking.value
  if (!target) return
  // 从列表按钮直接点击时，先关闭抽屉防止 dialog 关闭后抽屉闪现
  if (item) {
    closeBookingDrawer()
  }
  auditAction.value = action
  auditTarget.value = target
  auditReason.value = ''
  auditDialogTitle.value = `${action}预约 — ${target.bookingNo}`
  auditDialogVisible.value = true
}

async function confirmAudit() {
  const target = auditTarget.value
  if (!target) return

  if (auditAction.value === '驳回' && !auditReason.value.trim()) {
    ElMessage.warning('驳回时必须填写原因')
    return
  }

  auditing.value = true
  try {
    const isApproved = auditAction.value === '通过'
    const path = isApproved
      ? `/admin/bookings/${target.id}/approve`
      : `/admin/bookings/${target.id}/reject`
    await request.patch(path, {
      orderId: target.id,
      status: isApproved ? 1 : 2,
      reason: auditReason.value.trim() || '',
    })
    ElMessage.success(`审核${auditAction.value}成功`)
    // 先关闭弹层，再更新本地数据
    closeBookingDrawer()
    closeAuditDialog()
    // 本地更新状态，避免全量刷新导致闪烁
    const idx = bookings.value.findIndex(b => b.id === target.id)
    if (idx !== -1) {
      bookings.value[idx] = {
        ...bookings.value[idx],
        status: isApproved ? 'approved' : 'rejected',
      }
    }
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || `审核${auditAction.value}失败`)
  } finally {
    auditing.value = false
  }
}

function statusTag(status: BookingStatus) {
  return { pending: 'warning', approved: 'success', rejected: 'danger', completed: 'info', cancelled: 'info' }[status]
}

function statusText(status: BookingStatus) {
  return { pending: '待审核', approved: '已通过', rejected: '已驳回', completed: '已完成', cancelled: '已取消' }[status]
}
function campusName(c?: string) {
  return c === 'cq' ? '仓前' : c === 'xs' ? '下沙' : ''
}
</script>

<style scoped lang="scss">
.admin-hero {
  position: relative; display: grid; grid-template-columns: 1.2fr 0.8fr; gap: 20px;
  padding: 32px; border-radius: 30px; color: #fff;
  background: linear-gradient(135deg, #0f172a, #132949 55%, #3FB6FF);
  box-shadow: var(--shadow-card); overflow: hidden;
}
.admin-hero::before {
  content:""; position:absolute; inset:0;
  background: radial-gradient(circle at 18% 20%, rgba(255,255,255,.12), transparent 18%),
              linear-gradient(140deg, transparent 14%, rgba(255,255,255,.08) 42%, transparent 72%);
}
.admin-hero::after {
  content:""; position:absolute; inset:-30% -6% auto auto; width:280px; height:280px; border-radius:50%;
  background: radial-gradient(circle, rgba(123,208,255,.24), rgba(123,208,255,0));
  animation: adminGlow 8s ease-in-out infinite; pointer-events:none;
}
.admin-hero__main, .admin-hero__signal { position:relative; z-index:1; }
.hero-chip { display:inline-flex; padding:6px 12px; border-radius:999px; font-size:12px; letter-spacing:.08em; background:rgba(255,255,255,.12); margin-bottom:14px; }
.admin-hero h1 { margin:12px 0 10px; font-size:36px; line-height:1.18; }
.admin-hero p { max-width:740px; margin:0; line-height:1.8; color:rgba(255,255,255,.82); }
.admin-hero__signal { display:grid; gap:12px; }
.signal-card { display:grid; gap:4px; padding:16px 18px; border-radius:16px; background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.1); cursor:pointer; transition:background .2s; }
.signal-card:hover { background:rgba(255,255,255,.14); }
.signal-card span { font-size:13px; color:rgba(255,255,255,.64); }
.signal-card strong { font-size:26px; font-weight:700; }
.signal-card small { font-size:12px; color:rgba(255,255,255,.5); }

@keyframes adminGlow { 0%,100%{ transform:translate3d(0,0,0) scale(1); } 50%{ transform:translate3d(-16px,-8px,0) scale(1.06); } }
.booking-stack, .dialog-list, .drawer-stack { display: grid; gap: 14px; }

/* 分页栏固定在列表左下角 */
.list-pagination {
  display: flex;
  justify-content: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--border-soft);
}
.booking-item { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 18px 20px; border-radius: 20px; border: 1px solid var(--border-soft); background: linear-gradient(180deg, #fff, #F9FCFF); cursor: pointer; transition: transform .24s ease, box-shadow .24s ease, border-color .24s ease; }
.booking-item:hover { transform: translateY(-4px); box-shadow: 0 18px 28px rgba(20,33,61,.1); border-color: rgba(63,182,255,.14); }
.booking-item__main { flex: 1; display: grid; gap: 10px; }
.booking-item__head { display: flex; justify-content: space-between; gap: 12px; }
.head-tags { display: inline-flex; align-items: center; gap: 8px; flex-shrink: 0; }
.booking-item__head p { margin: 4px 0 0; color: var(--text-tertiary); font-size: 12px; }
.booking-item__meta { display: flex; flex-wrap: wrap; gap: 12px; color: var(--text-secondary); font-size: 13px; }
.booking-item__action, .button-row { display: flex; gap: 10px; }
.dialog-card { padding: 16px; border-radius: 18px; background: linear-gradient(180deg, #fff, #F9FCFF); border: 1px solid var(--border-soft); }
@media (max-width: 960px) { .admin-hero { grid-template-columns: 1fr; } .booking-item, .booking-item__head, .booking-item__action, .button-row { flex-direction: column; align-items: stretch; } }
</style>
