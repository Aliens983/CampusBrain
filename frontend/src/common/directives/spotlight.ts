/**
 * 全局鼠标追光（spotlight）控制器
 *
 * 原理：document 上只挂一个 passive pointermove（事件委托 + rAF 节流），
 * 命中卡片 / 按钮 / hero / 导航项等宿主时，懒注入两层光效 DOM：
 *   .spot-flood  跟随光标的柔和内发光（位于内容之下，不影响阅读）
 *   .spot-ring   跟随光标的边框辉光（mask 裁剪为 1px 描边）
 * 宿主加 `spot-tilt` class 后，额外获得随光标变化的轻微 3D 倾斜。
 *
 * 兼容性：
 * - 仅在 (hover: hover) and (pointer: fine) 的设备启用，触屏自动跳过
 * - prefers-reduced-motion: reduce 时关闭倾斜，仅保留静态光效
 */

/** 追光宿主选择器：新增可复用组件时，把 class 加到这里即可全局生效 */
const HOST_SELECTOR = [
  '.metric-card',
  '.shortcut-card',
  '.resource-card',
  '.panel-card',
  '.el-card',
  '.dashboard-hero',
  '.page-hero',
  '.auth-hero',
  '.campus-card',
  '.nav__item',
  '.el-button:not(.is-text):not(.is-link):not(.is-disabled)',
].join(',')

const ACTIVE_CLASS = 'is-spotlit'
const MAX_NESTED_HOSTS = 4
/** 倾斜最大角度（deg），保持克制，避免数据卡片过度晃动 */
const MAX_TILT = 6

interface HostMeta {
  flood: HTMLSpanElement
  ring: HTMLSpanElement
  tilt: boolean
}

/** 已注入光效层的宿主缓存（元素销毁后随 WeakMap 自动回收） */
const metas = new WeakMap<Element, HostMeta>()

interface ScheduledMove {
  clientX: number
  clientY: number
  /** composedPath() 必须在事件派发期间同步抓取；派发结束后再调用只会得到空数组 */
  path: EventTarget[]
}

let rafId = 0
let lastEvent: ScheduledMove | null = null
let activeHosts = new Set<HTMLElement>()

function finePointer(): boolean {
  return window.matchMedia('(hover: hover) and (pointer: fine)').matches
}

function reducedMotion(): boolean {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

/** 首次进入宿主时注入两层光效（prepend，不影响既有子元素顺序与 Vue 补丁） */
function ensureLayers(host: Element): HostMeta {
  const cached = metas.get(host)
  if (cached) {
    // 极端情况下宿主被 Vue 重绘，光效层丢失则补回
    if (!host.contains(cached.flood)) {
      host.prepend(cached.ring, cached.flood)
    }
    return cached
  }

  const flood = document.createElement('span')
  flood.className = 'spot-flood'
  flood.setAttribute('aria-hidden', 'true')

  const ring = document.createElement('span')
  ring.className = 'spot-ring'
  ring.setAttribute('aria-hidden', 'true')

  host.prepend(ring, flood)
  const meta: HostMeta = {
    flood,
    ring,
    tilt: host.classList.contains('spot-tilt'),
  }
  metas.set(host, meta)
  return meta
}

function applySpotlight(host: HTMLElement, clientX: number, clientY: number) {
  const rect = host.getBoundingClientRect()
  const x = clientX - rect.left
  const y = clientY - rect.top
  const meta = ensureLayers(host)

  host.style.setProperty('--spot-x', `${x.toFixed(1)}px`)
  host.style.setProperty('--spot-y', `${y.toFixed(1)}px`)

  if (meta.tilt && !reducedMotion() && rect.width > 0 && rect.height > 0) {
    const ry = ((x / rect.width) - 0.5) * 2 * MAX_TILT
    const rx = -((y / rect.height) - 0.5) * 2 * MAX_TILT
    host.style.transform =
      `perspective(900px) rotateX(${rx.toFixed(2)}deg) rotateY(${ry.toFixed(2)}deg) translateY(-5px)`
  }

  host.classList.add(ACTIVE_CLASS)
}

function releaseHost(host: HTMLElement) {
  host.classList.remove(ACTIVE_CLASS)
  if (metas.get(host)?.tilt) {
    // 交还样式表控制：scoped 的 :hover 过渡会平滑复位
    host.style.transform = ''
  }
}

function update(move: ScheduledMove) {
  const next = new Set<HTMLElement>()

  for (const node of move.path) {
    if (!(node instanceof HTMLElement)) continue
    if (node.matches?.(HOST_SELECTOR)) {
      next.add(node)
      if (next.size >= MAX_NESTED_HOSTS) break
    }
  }

  for (const host of next) {
    applySpotlight(host, move.clientX, move.clientY)
  }
  for (const host of activeHosts) {
    if (!next.has(host)) releaseHost(host)
  }
  activeHosts = next
  lastEvent = null
}

function schedule(move: ScheduledMove) {
  lastEvent = move
  if (rafId) return
  rafId = window.requestAnimationFrame(() => {
    rafId = 0
    if (lastEvent) update(lastEvent)
  })
}

function releaseAll() {
  for (const host of activeHosts) releaseHost(host)
  activeHosts = new Set<HTMLElement>()
  lastEvent = null
}

/** 滚动时光标相对元素位置变化，用最后一次事件刷新光效坐标 */
function onScroll(event: Event) {
  if (!lastEvent || activeHosts.size === 0) return
  schedule(lastEvent)
  void event
}

/* ============================================================
   鼠标流星拖尾（Meteor Trail）
   全屏固定 Canvas（pointer-events:none），鼠标移动时沿运动方向
   生成带亮头与渐变尾的流星粒子，加色混合（lighter）产生辉光。
   与卡片 spotlight 完全独立，互不影响。
   ============================================================ */

interface Meteor {
  x: number
  y: number
  vx: number
  vy: number
  /** 剩余寿命（以 60fps 帧为单位） */
  life: number
  maxLife: number
  size: number
  /** 头部主色（rgb 三元组，校徽蓝体系） */
  color: [number, number, number]
  /** 闪烁相位种子 */
  seed: number
}

function setupMeteorTrail(): void {
  if (typeof window === 'undefined') return
  if (!finePointer() || reducedMotion()) return

  const canvas = document.createElement('canvas')
  canvas.setAttribute('aria-hidden', 'true')
  canvas.style.cssText = [
    'position:fixed',
    'inset:0',
    'width:100%',
    'height:100%',
    'pointer-events:none',
    'z-index:9999',
  ].join(';')
  document.body.appendChild(canvas)

  const maybeCtx = canvas.getContext('2d')
  if (!maybeCtx) return
  // 取非空别名：嵌套函数内能稳定收窄为非空类型
  const ctx: CanvasRenderingContext2D = maybeCtx

  let dpr = 1
  function resize(): void {
    dpr = Math.min(window.devicePixelRatio || 1, 2)
    canvas.width = Math.round(window.innerWidth * dpr)
    canvas.height = Math.round(window.innerHeight * dpr)
  }
  resize()
  window.addEventListener('resize', resize)

  const meteors: Meteor[] = []
  const MAX_METEORS = 120

  /** 流星配色：亮白 / 校徽蓝 / 浅天蓝 / 冰蓝 */
  const PALETTE: Array<[number, number, number]> = [
    [255, 255, 255],
    [63, 182, 255],
    [123, 208, 255],
    [173, 226, 255],
  ]

  let lastX = 0
  let lastY = 0
  let hasAnchor = false

  let running = false
  let lastFrameTime = 0

  function spawn(x: number, y: number, mvx: number, mvy: number): void {
    const life = 22 + Math.random() * 20
    // 速度继承鼠标运动方向，加少量随机散布，形成向后飘散的尾迹
    const spread = 0.35
    meteors.push({
      x,
      y,
      vx: mvx * 0.28 + (Math.random() - 0.5) * spread,
      vy: mvy * 0.28 + (Math.random() - 0.5) * spread,
      life,
      maxLife: life,
      // 约 1/5 是更亮的“流星头”
      size: Math.random() < 0.2 ? 2.4 + Math.random() * 1.2 : 1.1 + Math.random() * 1.1,
      color: PALETTE[Math.floor(Math.random() * PALETTE.length)],
      seed: Math.random() * Math.PI * 2,
    })
    if (meteors.length > MAX_METEORS) meteors.splice(0, meteors.length - MAX_METEORS)
  }

  function onPointerMove(event: PointerEvent): void {
    const x = event.clientX
    const y = event.clientY

    // 首次事件只锚定坐标，避免从 (0,0) 拉出一条异常拖尾
    if (!hasAnchor) {
      lastX = x
      lastY = y
      hasAnchor = true
      return
    }

    const dx = x - lastX
    const dy = y - lastY
    const dist = Math.hypot(dx, dy)

    if (dist > 0.5) {
      // 按移动距离插值补点：快速甩动也有连续尾迹
      const steps = Math.min(Math.max(1, Math.round(dist / 7)), 5)
      for (let i = 1; i <= steps; i++) {
        const t = i / steps
        spawn(lastX + dx * t, lastY + dy * t, dx, dy)
      }
      start()
    }

    lastX = x
    lastY = y
  }

  /** 绘制单颗流星：translate 到位置后在局部坐标系绘制，尾迹沿速度反方向 */
  function drawMeteor(p: Meteor, now: number): void {
    const lifeRatio = p.life / p.maxLife
    const flicker = 0.72 + 0.28 * Math.sin(now * 0.02 + p.seed)
    const alpha = Math.max(0, Math.min(1, lifeRatio * flicker))
    const [cr, cg, cb] = p.color
    const speed = Math.hypot(p.vx, p.vy)
    const angle = Math.atan2(p.vy, p.vx)
    // 尾巴长度随速度与寿命变化
    const tailLen = Math.min(p.size * 9, 6 + speed * 1.6) * (0.4 + lifeRatio * 0.6)

    ctx.save()
    ctx.translate(p.x, p.y)
    ctx.rotate(angle)

    // 尾迹：从亮头向后渐隐的光带
    const tail = ctx.createLinearGradient(0, 0, -tailLen, 0)
    tail.addColorStop(0, `rgba(${cr},${cg},${cb},${0.85 * alpha})`)
    tail.addColorStop(0.4, `rgba(${cr},${cg},${cb},${0.35 * alpha})`)
    tail.addColorStop(1, `rgba(${cr},${cg},${cb},0)`)
    ctx.strokeStyle = tail
    ctx.lineWidth = p.size * 0.9
    ctx.lineCap = 'round'
    ctx.beginPath()
    ctx.moveTo(0, 0)
    ctx.lineTo(-tailLen, 0)
    ctx.stroke()

    // 头部辉光
    const glowR = p.size * 4
    const glow = ctx.createRadialGradient(0, 0, 0, 0, 0, glowR)
    glow.addColorStop(0, `rgba(255,255,255,${0.95 * alpha})`)
    glow.addColorStop(0.25, `rgba(${cr},${cg},${cb},${0.6 * alpha})`)
    glow.addColorStop(1, `rgba(${cr},${cg},${cb},0)`)
    ctx.fillStyle = glow
    ctx.beginPath()
    ctx.arc(0, 0, glowR, 0, Math.PI * 2)
    ctx.fill()

    // 亮芯
    ctx.fillStyle = `rgba(255,255,255,${alpha})`
    ctx.beginPath()
    ctx.arc(0, 0, p.size * 0.55, 0, Math.PI * 2)
    ctx.fill()

    ctx.restore()
  }

  function frame(now: number): void {
    // 时间步归一化到 60fps，高刷 / 低刷设备表现一致
    const dt = lastFrameTime ? Math.min((now - lastFrameTime) / 16.67, 3) : 1
    lastFrameTime = now

    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    ctx.clearRect(0, 0, window.innerWidth, window.innerHeight)
    ctx.globalCompositeOperation = 'lighter'

    const friction = Math.pow(0.92, dt)

    for (let i = meteors.length - 1; i >= 0; i--) {
      const p = meteors[i]
      p.life -= dt
      if (p.life <= 0) {
        meteors.splice(i, 1)
        continue
      }
      p.x += p.vx * dt
      p.y += p.vy * dt
      p.vx *= friction
      p.vy *= friction
      // 极轻微下沉，带出流星的坠落感
      p.vy += 0.035 * dt
      drawMeteor(p, now)
    }

    ctx.globalCompositeOperation = 'source-over'

    if (meteors.length > 0) {
      window.requestAnimationFrame(frame)
    } else {
      running = false
    }
  }

  function start(): void {
    if (running) return
    running = true
    lastFrameTime = 0
    window.requestAnimationFrame(frame)
  }

  document.addEventListener('pointermove', onPointerMove, { passive: true })
}

let installed = false

/** 安装全局追光（App 启动时调用一次，幂等） */
export function setupSpotlight(): void {
  if (installed || typeof window === 'undefined') return
  if (!finePointer()) return
  installed = true

  const onPointerMove = (event: Event): void => {
    const pointer = event as PointerEvent
    schedule({
      clientX: pointer.clientX,
      clientY: pointer.clientY,
      path: typeof pointer.composedPath === 'function' ? pointer.composedPath() : [],
    })
  }
  document.addEventListener('pointermove', onPointerMove, { passive: true })
  // 光标离开窗口 / 拖出浏览器时复位所有宿主
  document.documentElement.addEventListener('pointerleave', releaseAll)
  window.addEventListener('blur', releaseAll)
  window.addEventListener('scroll', onScroll, { passive: true, capture: true })

  // 鼠标流星拖尾（独立 Canvas 层，不影响上述卡片光效）
  setupMeteorTrail()
}
