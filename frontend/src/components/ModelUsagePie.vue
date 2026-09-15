<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { fetchModelUsage, type ModelUsageItem } from '../api/dashboard'

const chartRef = ref<HTMLDivElement | null>(null)
const loading = ref(false)
const hasError = ref(false)
const hasData = ref(true)
let chart: echarts.ECharts | null = null

onMounted(async () => {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  loading.value = true
  hasError.value = false
  try {
    const data: ModelUsageItem[] = await fetchModelUsage()
    hasData.value = data.length > 0
    if (hasData.value) {
      chart.setOption({
        title: { text: '模型占比', left: 'center' },
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        series: [{
          name: '模型占比',
          type: 'pie',
          radius: '60%',
          data: data.map(d => ({ name: d.model, value: d.totalTokens }))
        }]
      })
    }
  } catch (e) {
    console.error('Failed to fetch model usage', e)
    hasError.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="model-pie">
    <el-skeleton v-if="loading" :rows="5" animated />
    <el-alert v-else-if="hasError" title="加载失败，请检查后端服务" type="error" :closable="false" show-icon />
    <el-empty v-else-if="!hasData" description="暂无模型数据" />
    <div v-show="!loading && !hasError && hasData" ref="chartRef" style="width: 100%; height: 320px;"></div>
  </div>
</template>

<style scoped>
.model-pie {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #e5e7eb;
  margin-bottom: 24px;
}
</style>
