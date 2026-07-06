import request from './request'

export function submitInteraction(body) {
  return request.post('/interaction', body)
}

export function fetchAntiSpamReport() {
  return request.get('/anti-spam/report')
}

export function fetchCacheStats() {
  return request.get('/perf/cache-comparison')
}
