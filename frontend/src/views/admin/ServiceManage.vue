<script setup lang="ts">
import type { ServiceCard } from '@/common/types'
import { useServiceManage } from './composables/serviceManage'
import ServiceHero from './components/ServiceHero.vue'
import ServiceFilterBar from './components/ServiceFilterBar.vue'
import ServiceList from './components/ServiceList.vue'
import ServiceEditDrawer from './components/ServiceEditDrawer.vue'
import ServiceCreateDrawer from './components/ServiceCreateDrawer.vue'

/**
 * 服务治理页
 * <p>
 * 本文件只负责组装子组件；列表查询（服务端分页/筛选）、统计计数、编辑与新增
 * 等逻辑全部收敛在 {@link useServiceManage} 中（原为 580 行单文件）。
 */
const {
  services, total, keyword, statusFilter, campusFilter, pageNo, pageSize, loading,
  availableTotal, categories,
  createDrawer, selectedService, saving, editForm, createForm, serviceDrawerVisible,
  campusCount, onPageChange, onPageSizeChange, onSearch, onCampusChange,
  assetUrl, uploadImage, saveEdit, saveCreate,
  campusOptions
} = useServiceManage()

function onEdit(item: ServiceCard) { selectedService.value = item }
function onUploadEdit(file: File) { void uploadImage(file, 'edit') }
function onUploadCreate(file: File) { void uploadImage(file, 'create') }
</script>

<template>
  <div class="page-shell">
    <ServiceHero :total="total" :available-total="availableTotal" @create="createDrawer = true" />

    <el-card class="panel-card">
      <template #header>
        <div class="section-head">
          <h3 class="section-head__title">服务列表</h3>
        </div>
      </template>

      <ServiceFilterBar
        v-model:keyword="keyword"
        v-model:status-filter="statusFilter"
        :campus-filter="campusFilter"
        :campus-options="campusOptions"
        :campus-count="campusCount"
        @search="onSearch"
        @campus-change="onCampusChange"
      />

      <ServiceList
        :services="services"
        :loading="loading"
        :total="total"
        :page-no="pageNo"
        :page-size="pageSize"
        :asset-url="assetUrl"
        @edit="onEdit"
        @page-change="onPageChange"
        @size-change="onPageSizeChange"
      />
    </el-card>

    <ServiceEditDrawer
      v-model:visible="serviceDrawerVisible"
      :service="selectedService"
      :form="editForm"
      :saving="saving"
      :asset-url="assetUrl"
      @upload="onUploadEdit"
      @save="saveEdit"
    />

    <ServiceCreateDrawer
      v-model:visible="createDrawer"
      :form="createForm"
      :categories="categories"
      :saving="saving"
      :asset-url="assetUrl"
      @upload="onUploadCreate"
      @save="saveCreate"
    />
  </div>
</template>
