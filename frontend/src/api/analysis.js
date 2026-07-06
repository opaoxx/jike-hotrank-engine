import request from './request'

export function fetchOverview() {
  return request.get('/analysis/overview')
}

export function fetchHeatDistribution() {
  return request.get('/analysis/heat-distribution')
}

export function fetchInteractionStats(hours = 24) {
  return request.get('/analysis/interaction-stats', { params: { hours } })
}

export function fetchCircleActivity() {
  return request.get('/analysis/circle-activity')
}

export function fetchAntiCheatStats(days = 7) {
  return request.get('/analysis/anti-cheat-stats', { params: { days } })
}
