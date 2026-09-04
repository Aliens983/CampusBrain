<template>
  <router-view v-slot="{ Component, route }">
    <Suspense>
      <template #default>
        <transition
          name="route-fade"
          mode="out-in"
        >
          <!-- key 用“顶层布局”，而不是 route.fullPath：
               否则在 UserLayoutShell 内部切子路由（dashboard/services/...）会因
               fullPath 变化把整个布局卸载重挂 → 白屏 + 头部/面板闪烁位移。
               用 matched[0].path 只在 登录↔用户端↔管理端 切换时才触发过渡，
               布局内部仅切换内容，面板保持不动 -->
          <component
            :is="Component"
            :key="route.matched[0]?.path ?? route.path"
          />
        </transition>
      </template>
      <template #fallback>
        <RouteLoading />
      </template>
    </Suspense>
  </router-view>
</template>

<script setup lang="ts">
import RouteLoading from '@/components/RouteLoading.vue'
</script>
