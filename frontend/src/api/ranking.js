import request from './request'

export function fetchGlobalRanking(limit = 50) {
  return request.get('/ranking/global', { params: { limit } })
}

export function fetchCircleRanking(circleId, limit = 20) {
  return request.get(`/ranking/circle/${circleId}`, { params: { limit } })
}

export function fetchNewcomerRanking(limit = 10) {
  return request.get('/ranking/newcomer', { params: { limit } })
}

export function fetchSurgingRanking(limit = 10) {
  return request.get('/ranking/surging', { params: { limit } })
}

export function fetchPersonalizedRanking(userId, limit = 50) {
  return request.get('/ranking/personalized', { params: { userId, limit } })
}
