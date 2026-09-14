<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { fetchSummary } from '../api/dashboard'

const totalTokens = ref(0)
const requestCount = ref(0)
const modelCount = ref(0)

onMounted(async () => {
  try {
    const data = await fetchSummary()
    totalTokens.value = data.totalTokens
    requestCount.value = data.requestCount
    modelCount.value = data.modelCount
  } catch (e) {
    console.error('Failed to fetch summary', e)
  }
})
</script>

<template>
  <div class="summary-cards">
    <div class="card">
      <div class="card-label">今日 Token</div>
      <div class="card-value">{{ totalTokens }}</div>
    </div>
    <div class="card">
      <div class="card-label">今日请求数</div>
      <div class="card-value">{{ requestCount }}</div>
    </div>
    <div class="card">
      <div class="card-label">今日模型数</div>
      <div class="card-value">{{ modelCount }}</div>
    </div>
  </div>
</template>

<style scoped>
.summary-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #e5e7eb;
}
.card-label {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
}
.card-value {
  font-size: 28px;
  font-weight: 600;
  color: #111827;
}
</style>
