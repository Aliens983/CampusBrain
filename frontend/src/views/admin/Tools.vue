<template>
  <div class="page-shell">
    <section class="admin-hero">
      <div class="admin-hero__main">
        <p class="hz-eyebrow">
          Campus utilities · HZNU
        </p>
        <h1>工具箱</h1>
        <p class="admin-hero__desc">
          天气、二维码与文件上传等日常工具，集中在一处。
        </p>
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
            <el-icon><Sunny /></el-icon>
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
            <el-icon><component :is="weatherIcon(weather.weather1)" /></el-icon>
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
            <el-icon><Postcard /></el-icon>
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
          <el-icon><Cloudy /></el-icon>
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
import type { UploadFile, UploadUserFile } from 'element-plus'
import { Cloudy, Postcard, Sunny, UploadFilled } from '@element-plus/icons-vue'
import request from '@/common/utils/request'
import axios from 'axios'

// ====== Weather ======
const weatherSheng = ref('')
const weatherPlace = ref('')
const weatherLoading = ref(false)
type WeatherResult = { shi: string; qu: string; weather1: string; temp: string; name: string }
const weather = ref<WeatherResult | null>(null)

async function fetchWeather() {
  if (!weatherSheng.value || !weatherPlace.value) { ElMessage.warning('请输入省份和城市'); return }
  weatherLoading.value = true
  try {
    weather.value = (await request.get('/weather', { params: { sheng: weatherSheng.value, place: weatherPlace.value } })) as WeatherResult | null
  } catch { ElMessage.error('天气查询失败') }
  finally { weatherLoading.value = false }
}

function weatherIcon(desc: string) {
  if (!desc) return 'Sunny'
  if (desc.includes('晴')) return 'Sunny'
  if (desc.includes('云')) return 'Cloudy'
  if (desc.includes('雨')) return 'Drizzling'
  if (desc.includes('雪')) return 'Cloudy'
  return 'PartlyCloudy'
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
    // 二维码图片使用与文件服务一致的同源 /uploads 路径。
    const path = new URL(imageUrl).pathname.replace('/api/v1', '')
    qrImage.value = path
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
const ossFileList = ref<UploadUserFile[]>([])
const ossLoading = ref(false)
const ossUploaded = ref(false)

function handleFileChange(file: UploadFile) {
  if (!file.raw) return
  ossFile.value = file.raw as File
  ossFileList.value = [file]
  ossUploaded.value = false
}
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
.tools-card {
  margin-bottom: 20px;
  padding: 22px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-card);
}

.tools-card__head {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
}

.tools-card__icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  color: var(--lake);
  font-size: 21px;
}

.tools-card__head h3 {
  margin: 0 0 2px;
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 600;
  color: var(--ink);
}

.tools-card__head p {
  margin: 0;
  color: var(--ink-3);
  font-size: 11.5px;
}

.tools-form {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.tools-form .el-input {
  flex: 1 1 0;
  min-width: 0;
}

.tools-form:not(.tools-form--wide) .el-button {
  flex: 0 0 auto;
  min-width: 118px;
  margin: 0;
}

.tools-form--wide {
  flex-direction: column;
  align-items: stretch;
  gap: 16px;
}

.weather-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 18px 20px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-2);
}

.weather-card__icon {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface);
  color: var(--lake);
  font-size: 26px;
  line-height: 1;
}

.weather-card__info strong {
  font-family: var(--font-serif);
  font-size: 16px;
  color: var(--ink);
}

.weather-card__main {
  margin: 4px 0;
  font-size: 13px;
  color: var(--ink-3);
}

.weather-card__main b {
  margin-left: 6px;
  font-family: var(--font-serif);
  font-size: 19px;
  color: var(--lake);
}

.weather-card__info small {
  color: var(--ink-4);
  font-size: 11px;
}

.qr-result {
  display: grid;
  place-items: center;
  gap: 14px;
  padding: 10px;
}

.qr-result img {
  width: 190px;
  height: 190px;
  padding: 8px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface);
}

.oss-result {
  display: grid;
  gap: 6px;
  margin-top: 14px;
}

.oss-result__label {
  color: var(--ink-3);
  font-size: 12px;
}

.span-6 {
  grid-column: span 6;
}

@media (max-width: 960px) {
  .span-6 {
    grid-column: span 12;
  }
}
</style>
