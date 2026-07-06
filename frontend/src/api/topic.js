import request from './request'

export function fetchTopicDetail(id) {
  return request.get(`/topic/${id}`)
}

export function blockTopic(id) {
  return request.post(`/topic/${id}/block`)
}

export function unblockTopic(id) {
  return request.post(`/topic/${id}/unblock`)
}
