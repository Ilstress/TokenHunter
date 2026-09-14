import axios from 'axios'

const http = axios.create({
  baseURL: 'http://127.0.0.1:8080',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

export interface SummaryResponse {
  totalTokens: number
  requestCount: number
  modelCount: number
}

export interface TrendPoint {
  day: string
  totalTokens: number
}

export interface ModelUsageItem {
  model: string
  totalTokens: number
  percentage: number
}

export interface ProviderUsageItem {
  provider: string
  totalTokens: number
  percentage: number
}

export function fetchSummary(): Promise<SummaryResponse> {
  return http.get('/api/dashboard/summary').then(r => r.data)
}

export function fetchTrend(range: '7d' | '30d' = '7d'): Promise<TrendPoint[]> {
  return http.get('/api/dashboard/trend', { params: { range } }).then(r => r.data)
}

export function fetchModelUsage(): Promise<ModelUsageItem[]> {
  return http.get('/api/dashboard/model-usage').then(r => r.data)
}

export function fetchProviderUsage(): Promise<ProviderUsageItem[]> {
  return http.get('/api/dashboard/provider-usage').then(r => r.data)
}
