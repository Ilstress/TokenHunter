<script setup lang="ts">
import SummaryCards from '../components/SummaryCards.vue'
import TokenTrendChart from '../components/TokenTrendChart.vue'
import ModelUsagePie from '../components/ModelUsagePie.vue'
import ProviderUsagePie from '../components/ProviderUsagePie.vue'
import RecentRequestsTable from '../components/RecentRequestsTable.vue'
</script>

<template>
  <div class="dashboard">
    <SummaryCards />
    <TokenTrendChart />
    <ModelUsagePie />
    <ProviderUsagePie />
    <RecentRequestsTable />
  </div>
</template>

<style scoped>
.dashboard {
  padding: 24px;
  max-width: 1280px;
  margin: 0 auto;
}
</style>
