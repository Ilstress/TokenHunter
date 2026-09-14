import axios from 'axios'

const http = axios.create({
  baseURL: 'http://127.0.0.1:8080',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

export interface UsageRecord {
  id: number
  provider: string
  model: string
  inputTokens: number
  cacheHitTokens: number
  cacheMissTokens: number
  outputTokens: number
  totalTokens: number
  durationMs: number
  status: number
  httpStatus: number | null
  errorMessage: string | null
  endpoint: string | null
  stream: number
  createdAt: string
}

export function fetchUsageRecords(): Promise<UsageRecord[]> {
  return http.get('/api/usage-records').then(r => r.data)
}
