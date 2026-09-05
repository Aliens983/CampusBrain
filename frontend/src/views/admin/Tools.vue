<template>
  <div class="page-shell">
    <section class="admin-hero">
      <div class="admin-hero__main">
        <h1>工具箱</h1>
      </div>
      <div class="admin-hero__signal">
        <div
          class="signal-card"
          @click="scrollTo('weather')"
        >
          <span>天气查询</span><strong>实时</strong><small>全国城市</small>
        </div>
      </div>
    </section>

    <section class="grid-cards">
      <!-- 天气 -->
      <div
        id="weather"
        class="tools-card span-6"
      >
        <div class="tools-card__head">
          <div class="tools-card__icon">
            🌤
          </div>
          <div>
            <h3>天气查询</h3>
            <p>实时查询全国城市天气信息</p>
          </div>
        </div>
        <div class="tools-form">
          <el-input
            v-model="weatherSheng"
            size="large"
            placeholder="省份，如：广东"
          />
          <el-input
            v-model="weatherPlace"
            size="large"
            placeholder="城市，如：广州"
          />
          <el-button
            type="primary"
            size="large"
            :loading="weatherLoading"
            @click="fetchWeather"
          >
            查询
          </el-button>
        </div>
        <div
          v-if="weather"
          class="weather-card"
        >
          <div class="weather-card__icon">
            {{ weatherIcon(weather.weather1) }}
          </div>
          <div class="weather-card__info">
            <strong>{{ weather.shi }} {{ weather.qu }}</strong>
            <div class="weather-card__main">
              {{ weather.weather1 }} <b>{{ weather.temp }}</b>
            </div>
            <small>{{ weather.name }}</small>
          </div>
        </div>
      </div>

      <!-- 二维码 -->
      <div
        id="qr"
        class="tools-card span-6"
      >
        <div class="tools-card__head">
          <div class="tools-card__icon">
            📱
          </div>
          <div>
            <h3>二维码生成</h3>
            <p>输入文本或链接，生成可下载的二维码</p>
          </div>
        </div>
        <div class="tools-form">
          <el-input
            v-model="qrContent"
            size="large"
            placeholder="输入文本或链接地址"
          />
          <el-button
            type="primary"
            size="large"
            :loading="qrLoading"
            @click="generateQr"
          >
            生成
          </el-button>
        </div>
        <div
          v-if="qrImage"
          class="qr-result"
        >
          <img
            :src="qrImage"
            alt="QR Code"
          >
          <el-button
            size="small"
            plain
            @click="downloadQr"
          >
            下载 PNG
          </el-button>
        </div>
      </div>
    </section>

    <!-- OSS -->
    <div
      id="oss"
      class="tools-card"
    >
      <div class="tools-card__head">
        <div class="tools-card__icon">
          ☁️
        </div>
        <div>
          <h3>OSS 文件上传</h3>
          <p>上传文件到阿里云 OSS，获取可公开访问的 URL</p>
        </div>
      </div>
      <div class="tools-form tools-form--wide">
        <el-upload
          :auto-upload="false"
          :limit="1"
          :on-change="handleFileChange"
          :file-list="ossFileList"
          list-type="picture"
          drag
        >
          <el-icon class="el-icon--upload">
            <UploadFilled />
          </el-icon>
          <div class="el-upload__text">
            拖拽文件到此处或<em>点击选择</em>
          </div>
        </el-upload>
        <el-button
          type="primary"
          :loading="ossLoading"
          :disabled="!ossFile"
          @click="uploadOss"
        >
          上传到 OSS
        </el-button>
      </div>
      <div
        v-if="ossUploaded"
        class="oss-result"
      >
        <el-alert
          title="文件上传成功"
          type="success"
          :closable="false"
          show-icon
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import request from '@/common/utils/request'
import axios from 'axios'

// ====== Weather ======
const weatherSheng = ref('')
const weatherPlace = ref('')
const weatherLoading = ref(false)
const weather = ref<{ shi: string; qu: string; weather1: string; temp: string; name: string } | null>(null)

async function fetchWeather() {
  if (!weatherSheng.value || !weatherPlace.value) { ElMessage.warning('请输入省份和城市'); return }
  weatherLoading.value = true
  try {
    weather.value = await request.get('/weather', { params: { sheng: weatherSheng.value, place: weatherPlace.value } }) as any
  } catch { ElMessage.error('天气查询失败') }
  finally { weatherLoading.value = false }
}

function weatherIcon(desc: string) {
  if (!desc) return '🌈'
  if (desc.includes('晴')) return '☀️'
  if (desc.includes('云')) return '⛅'
  if (desc.includes('雨')) return '🌧'
  if (desc.includes('雪')) return '🌨'
  return '🌈'
}

// ====== QR Code ======
const qrContent = ref('')
const qrLoading = ref(false)
const qrImage = ref('')

async function generateQr() {
  if (!qrContent.value) { ElMessage.warning('请输入二维码内容'); return }
  qrLoading.value = true
  try {
    const imageUrl = await request.get('/app/qr-code', { params: { content: qrContent.value } }) as string
    // 后端返回 http://localhost:18080/api/v1/uploads/xxx.png
    // 转为走Vite代理的路径 /api/uploads/xxx.png
    const path = new URL(imageUrl).pathname.replace('/api/v1', '')
    qrImage.value = '/api' + path
  } catch { ElMessage.error('二维码生成失败') }
  finally { qrLoading.value = false }
}

function downloadQr() {
  if (!qrImage.value) return
  // 直接下载图片URL
  const a = document.createElement('a'); a.href = qrImage.value; a.download = 'qrcode.png'; a.click()
}

// ====== OSS ======
const ossFile = ref<File | null>(null)
const ossFileList = ref<any[]>([])
const ossLoading = ref(false)
const ossUploaded = ref(false)

function handleFileChange(file: any) { ossFile.value = file.raw; ossFileList.value = [file]; ossUploaded.value = false }
async function uploadOss() {
  if (!ossFile.value) return
  ossLoading.value = true
  try {
    const form = new FormData(); form.append('file', ossFile.value)
    await request.post('/admin/files/oss', form)
    ossUploaded.value = true
    ElMessage.success('文件上传成功')
  } catch { ElMessage.error('文件上传失败，请检查 OSS 配置') }
  finally { ossLoading.value = false }
}

function scrollTo(id: string) { document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' }) }

// 判断字符串是否含中文字符
function isChinese(s: string) { return /[一-龥]/.test(s) }

// 自动定位天气
onMounted(async () => {
  try {
    const ipRes = await axios.get('http://ip-api.com/json/?lang=zh-CN')
    const { city, regionName } = ipRes.data as { city: string; regionName: string }
    // ip-api 对部分 IP 返回英文城市名（如 Kandun），而天气 API 只认中文，需校验
    if (city && regionName && isChinese(city) && isChinese(regionName)) {
      weatherSheng.value = regionName
      weatherPlace.value = city
      await fetchWeather()
    } else {
      // 定位不精确：只填中文省份，提示用户手动输入中文城市
      if (regionName && isChinese(regionName)) {
        weatherSheng.value = regionName
      }
      ElMessage.warning('自动定位城市不精确，请手动输入城市名称')
    }
  } catch { /* 静默 */ }
})
</script>

<style scoped lang="scss">
.admin-hero {
  position: relative; display: grid; grid-template-columns: 1.2fr 0.8fr; gap: 20px;
  padding: 32px; border-radius: 30px; color: #fff;
  background: linear-gradient(135deg, #0f172a, #132949 55%, #7c3aed);
  box-shadow: var(--shadow-card); overflow: hidden;
}
.admin-hero::before {
  content:""; position:absolute; inset:0;
  background: radial-gradient(circle at 18% 20%, rgba(255,255,255,.12), transparent 18%),
              linear-gradient(140deg, transparent 14%, rgba(255,255,255,.08) 42%, transparent 72%);
}
.admin-hero::after {
  content:""; position:absolute; inset:-30% -6% auto auto; width:280px; height:280px; border-radius:50%;
  background: radial-gradient(circle, rgba(139,92,246,.24), rgba(139,92,246,0));
  animation: adminGlow 8s ease-in-out infinite; pointer-events:none;
}
.admin-hero__main, .admin-hero__signal { position:relative; z-index:1; }
.hero-chip { display:inline-flex; padding:6px 12px; border-radius:999px; font-size:12px; letter-spacing:.08em; background:rgba(255,255,255,.12); margin-bottom:14px; }
.admin-hero h1 { margin:12px 0 10px; font-size:32px; line-height:1.18; }
.admin-hero p { max-width:740px; margin:0; line-height:1.8; color:rgba(255,255,255,.82); }
.admin-hero__signal { display:grid; gap:12px; }
.signal-card { display:grid; gap:4px; padding:16px 18px; border-radius:16px; background:rgba(255,255,255,.08); border:1px solid rgba(255,255,255,.1); cursor:pointer; transition:background .2s; }
.signal-card:hover { background:rgba(255,255,255,.14); }
.signal-card span { font-size:13px; color:rgba(255,255,255,.64); }
.signal-card strong { font-size:26px; font-weight:700; }
.signal-card small { font-size:12px; color:rgba(255,255,255,.5); }

@keyframes adminGlow { 0%,100%{ transform:translate3d(0,0,0) scale(1); } 50%{ transform:translate3d(-16px,-8px,0) scale(1.06); } }

.tools-card {
  background: #fff; border: 1px solid var(--border-soft); border-radius: 18px;
  padding: 24px; box-shadow: var(--shadow-card); margin-bottom: 20px;
}
.tools-card__head {
  display: flex; align-items: center; gap: 16px; margin-bottom: 20px; padding-bottom: 18px;
  border-bottom: 1px solid var(--border-soft);
}
.tools-card__icon { width: 48px; height: 48px; display: grid; place-items: center; border-radius: 14px; background: #f4f0fc; font-size: 24px; flex-shrink: 0; }
.tools-card__head h3 { margin: 0 0 2px; font-size: 17px; font-weight: 700; }
.tools-card__head p { margin: 0; color: var(--text-secondary); font-size: 13px; }

.chat-messages { height: 320px; overflow-y: auto; display: grid; gap: 14px; padding: 4px 0; margin-bottom: 16px; }
.chat-empty { display: grid; place-items: center; gap: 10px; padding: 40px; text-align: center; }
.chat-empty__icon { font-size: 40px; }
.chat-empty p { color: var(--text-tertiary); margin:0; }
.chat-hints { display: flex; gap: 8px; flex-wrap: wrap; justify-content: center; }
.chat-hints span { padding: 6px 14px; border-radius: 999px; background: #f4f0fc; color: var(--brand-500); font-size: 12px; cursor: pointer; transition: background .2s; }
.chat-hints span:hover { background: #e3dbfc; }

.chat-msg { display: flex; gap: 10px; align-items: flex-start; }
.chat-msg.user { flex-direction: row-reverse; }
.chat-msg__avatar { width: 34px; height: 34px; display: grid; place-items: center; border-radius: 50%; background: #f4f0fc; font-size: 16px; flex-shrink: 0; }
.chat-msg.user .chat-msg__avatar { background: #ebe3f9; }
.chat-msg__bubble {
  max-width: 70%; padding: 12px 16px; border-radius: 16px; font-size: 14px; line-height: 1.65;
  background: #f3f4f6; color: #1f2937; white-space: pre-wrap; word-break: break-word;
}
.chat-msg.user .chat-msg__bubble { background: linear-gradient(135deg, #8b5cf6, #7c3aed); color: #fff; }
.typing span { display:inline-block; width:6px; height:6px; border-radius:50%; background:#94a3b8; margin:0 2px; animation: typingBounce 1.4s ease-in-out infinite; }
.typing span:nth-child(2) { animation-delay: .2s; }
.typing span:nth-child(3) { animation-delay: .4s; }
@keyframes typingBounce { 0%,80%,100%{ transform:scale(.6); opacity:.5; } 40%{ transform:scale(1); opacity:1; } }

.chat-input { display: flex; gap: 10px; }

.tools-form { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
/* 输入框均分剩余宽度，按钮固定舒展不缩 */
.tools-form .el-input { flex: 1 1 0; min-width: 0; }
.tools-form:not(.tools-form--wide) .el-button {
  flex: 0 0 auto;
  min-width: 128px;
  margin: 0;
  font-weight: 600;
}
.tools-form--wide { flex-direction: column; align-items: stretch; gap: 16px; }

.weather-card { display: flex; gap: 18px; align-items: center; padding: 20px; border-radius: 18px; background: linear-gradient(135deg, #f4f0fc, #ecfeff); border: 1px solid #ddd6fe; }
.weather-card__icon { font-size: 52px; }
.weather-card__info strong { font-size: 17px; }
.weather-card__main { margin: 4px 0; font-size: 15px; color: var(--text-secondary); }
.weather-card__main b { color: var(--text-primary); font-size: 18px; margin-left: 6px; }
.weather-card__info small { color: var(--text-tertiary); }

.qr-result { display: grid; place-items: center; gap: 14px; padding: 10px; }
.qr-result img { width: 190px; height: 190px; border-radius: 14px; border: 1px solid var(--border-soft); padding: 8px; background: #fff; }

.oss-result { margin-top: 14px; display: grid; gap: 6px; }
.oss-result__label { font-size: 13px; color: var(--text-secondary); }

.span-6 { grid-column: span 6; }

@media (max-width: 960px) { .admin-hero { grid-template-columns: 1fr; } .span-6 { grid-column: span 12; } }
</style>
