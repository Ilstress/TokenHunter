<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'

const chartRef = ref<HTMLDivElement | null>(null)

onMounted(() => {
  if (!chartRef.value) return
  const chart = echarts.init(chartRef.value)
  chart.setOption({
    title: { text: 'Token 趋势（假数据）' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ['Day 1', 'Day 2', 'Day 3', 'Day 4', 'Day 5', 'Day 6', 'Day 7']
    },
    yAxis: { type: 'value' },
    series: [{
      name: 'Token',
      type: 'line',
      smooth: true,
      data: [120, 200, 150, 300, 280, 400, 350]
    }]
  })
})
</script>

<template>
  <div class="trend-chart">
    <div ref="chartRef" style="width: 100%; height: 360px;"></div>
  </div>
</template>

<style scoped>
.trend-chart {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #e5e7eb;
  margin-bottom: 24px;
}
</style>
