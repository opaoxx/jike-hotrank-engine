import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/overview' },
  { path: '/overview', name: 'Overview', component: () => import('../views/OverviewView.vue'), meta: { title: '📊 总览' } },
  { path: '/global', name: 'Global', component: () => import('../views/GlobalRankingView.vue'), meta: { title: '🌐 全站热榜' } },
  { path: '/circle', name: 'Circle', component: () => import('../views/CircleRankingView.vue'), meta: { title: '⭕ 圈子热榜' } },
  { path: '/newcomer', name: 'Newcomer', component: () => import('../views/NewcomerView.vue'), meta: { title: '🆕 新星榜' } },
  { path: '/surging', name: 'Surging', component: () => import('../views/SurgingView.vue'), meta: { title: '🚀 飙升榜' } },
  { path: '/anti-spam', name: 'AntiSpam', component: () => import('../views/AntiSpamView.vue'), meta: { title: '🛡️ 反作弊' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
