<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'
import { fetchTrend, type TrendPoint } from '../api/dashboard'

const chartRef = ref<HTMLDivElement | null>(null)
const range = ref<'7d' | '30d'>('7d')
let chart: echarts.ECharts | null = null

async function loadTrend() {
  if (!chart) return
  try {
    const data: TrendPoint[] = await fetchTrend(range.value)
    chart.setOption({
      title: { text: `Token 趋势（${range.value}）` },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: data.map(d => d.day)
      },
      yAxis: { type: 'value' },
      series: [{
        name: 'Token',
        type: 'line',
        smooth: true,
        data: data.map(d => d.totalTokens)
      }]
    })
  } catch (e) {
    console.error('Failed to fetch trend', e)
  }
}

onMounted(() => {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  loadTrend()
})

watch(range, () => {
  loadTrend()
})
</script>

<template>
  <div class="trend-chart">
    <div class="trend-header">
      <h3>Token 趋势</h3>
      <el-radio-group v-model="range" size="small">
        <el-radio-button value="7d">近 7 天</el-radio-button>
        <el-radio-button value="30d">近 30 天</el-radio-button>
      </el-radio-group>
    </div>
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
.trend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.trend-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
</style>
