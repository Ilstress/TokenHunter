<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { fetchProviderUsage, type ProviderUsageItem } from '../api/dashboard'

const chartRef = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null

onMounted(async () => {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  try {
    const data: ProviderUsageItem[] = await fetchProviderUsage()
    chart.setOption({
      title: { text: 'Provider 占比', left: 'center' },
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
      },
      series: [{
        name: 'Provider 占比',
        type: 'pie',
        radius: '60%',
        data: data.map(d => ({ name: d.provider, value: d.totalTokens }))
      }]
    })
  } catch (e) {
    console.error('Failed to fetch provider usage', e)
  }
})
</script>

<template>
  <div class="provider-pie">
    <div ref="chartRef" style="width: 100%; height: 320px;"></div>
  </div>
</template>

<style scoped>
.provider-pie {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #e5e7eb;
  margin-bottom: 24px;
}
</style>
