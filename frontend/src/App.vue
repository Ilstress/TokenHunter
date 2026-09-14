<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'

const chartRef = ref<HTMLDivElement | null>(null)

onMounted(() => {
  if (!chartRef.value) return
  const chart = echarts.init(chartRef.value)
  chart.setOption({
    title: { text: 'ECharts 测试图表' },
    xAxis: { type: 'category', data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'] },
    yAxis: { type: 'value' },
    series: [{ data: [120, 200, 150, 80, 70], type: 'line' }]
  })
})
</script>

<template>
  <div style="padding: 20px;">
    <h1>TokenHunter Dashboard</h1>
    <div ref="chartRef" style="width: 600px; height: 400px;"></div>
  </div>
</template>
