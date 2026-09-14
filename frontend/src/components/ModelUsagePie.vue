<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { fetchModelUsage, type ModelUsageItem } from '../api/dashboard'

const chartRef = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null

onMounted(async () => {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  try {
    const data: ModelUsageItem[] = await fetchModelUsage()
    chart.setOption({
      title: { text: '模型占比', left: 'center' },
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
      },
      series: [{
        name: '模型占比',
        type: 'pie',
        radius: '60%',
        data: data.map(d => ({ name: d.model, value: d.totalTokens }))
      }]
    })
  } catch (e) {
    console.error('Failed to fetch model usage', e)
  }
})
</script>

<template>
  <div class="model-pie">
    <div ref="chartRef" style="width: 100%; height: 320px;"></div>
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
