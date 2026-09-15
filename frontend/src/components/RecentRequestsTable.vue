<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { fetchUsageRecords, type UsageRecord } from '../api/usageRecord'

const records = ref<UsageRecord[]>([])
const loading = ref(false)
const hasError = ref(false)
const hasData = ref(true)

function formatDate(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  if (isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

onMounted(async () => {
  loading.value = true
  hasError.value = false
  try {
    const data = await fetchUsageRecords()
    const sorted = data.sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1)).slice(0, 20)
    records.value = sorted
    hasData.value = sorted.length > 0
  } catch (e) {
    console.error('Failed to fetch usage records', e)
    hasError.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="recent-requests">
    <h3 class="table-title">最近请求</h3>
    <el-skeleton v-if="loading" :rows="5" animated />
    <el-alert v-else-if="hasError" title="加载失败，请检查后端服务" type="error" :closable="false" show-icon />
    <el-empty v-else-if="!hasData" description="暂无请求记录" />
    <el-table v-else :data="records" stripe style="width: 100%;">
      <el-table-column prop="createdAt" label="时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="provider" label="Provider" width="120" />
      <el-table-column prop="model" label="模型" width="180" />
      <el-table-column prop="inputTokens" label="输入" width="80" align="right" />
      <el-table-column prop="outputTokens" label="输出" width="80" align="right" />
      <el-table-column prop="totalTokens" label="总计" width="100" align="right" />
      <el-table-column prop="durationMs" label="耗时(ms)" width="100" align="right" />
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
            {{ row.status === 0 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.recent-requests {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #e5e7eb;
  margin-bottom: 24px;
}
.table-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
}
</style>
