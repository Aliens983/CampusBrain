<template>
  <router-view v-slot="{ Component, route }">
    <Suspense>
      <template #default>
        <transition
          name="route-fade"
          mode="out-in"
        >
          <component
            :is="Component"
            :key="route.fullPath"
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
