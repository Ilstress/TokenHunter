<script setup lang="ts">
import SummaryCards from '../components/SummaryCards.vue'
import TokenTrendChart from '../components/TokenTrendChart.vue'
import ModelUsagePie from '../components/ModelUsagePie.vue'
import ProviderUsagePie from '../components/ProviderUsagePie.vue'
import RecentRequestsTable from '../components/RecentRequestsTable.vue'
</script>

<template>
  <div style="padding: 20px;">
    <SummaryCards />
    <TokenTrendChart />
    <ModelUsagePie />
    <ProviderUsagePie />
    <RecentRequestsTable />
  </div>
</template>
