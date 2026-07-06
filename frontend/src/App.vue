<script setup>
import { ref, watch, provide } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useSSE } from './composables/useSSE'
import { useAutoRefresh } from './composables/useAutoRefresh'
import { useAppStore } from './stores/app'
import InteractionForm from './components/InteractionForm.vue'

const router = useRouter()
const route = useRoute()
const store = useAppStore()

const tabs = [
  { path: '/overview', label: '📊 总览' },
  { path: '/global', label: '🌐 全站热榜' },
  { path: '/circle', label: '⭕ 圈子热榜' },
  { path: '/newcomer', label: '🆕 新星榜' },
  { path: '/surging', label: '🚀 飙升榜' },
  { path: '/anti-spam', label: '🛡️ 反作弊' }
]

const activeTab = ref('/overview')

watch(route, (r) => { activeTab.value = r.path })
watch(activeTab, (path) => { router.push(path) })

// 子组件注册刷新回调
const refreshCallbacks = new Set()

function registerRefresh(cb) {
  refreshCallbacks.add(cb)
  return () => refreshCallbacks.delete(cb)
}

function triggerAllRefresh() {
  refreshCallbacks.forEach(cb => {
    try { cb() } catch (e) { /* ignore */ }
  })
  store.loadCacheStats()
}

provide('registerRefresh', registerRefresh)

// SSE
const { connected, subscriberCount, on } = useSSE()

on('onRankingUpdated', (data) => {
  ElMessage.info(`榜单已更新：${data.updatedTopicCount} 个话题`)
  triggerAllRefresh()
})

on('onTopNEntered', (data) => {
  ElMessage.success(`🏆 新晋前 ${data.threshold} 名：「${data.title}」`)
})

// 自动刷新
const { secondsLeft } = useAutoRefresh(triggerAllRefresh)

// 初始加载缓存统计
store.loadCacheStats()
</script>

<template>
  <div class="app-layout">
    <!-- 导航栏 -->
    <header class="app-navbar">
      <span class="brand">🔥 即刻热点引擎</span>
      <div class="navbar-right">
        <el-tag :type="connected ? 'success' : 'danger'" size="small" effect="dark">
          SSE {{ connected ? `● 已连接 (${subscriberCount})` : '● 已断开' }}
        </el-tag>
        <span class="refresh-timer">刷新：{{ secondsLeft }}s</span>
      </div>
    </header>

    <!-- Tab 栏 -->
    <nav class="app-tabs">
      <el-tabs v-model="activeTab" type="card">
        <el-tab-pane
          v-for="tab in tabs"
          :key="tab.path"
          :label="tab.label"
          :name="tab.path"
        />
      </el-tabs>
    </nav>

    <!-- 主内容 -->
    <main class="app-main">
      <router-view />
    </main>

    <!-- 底部互动模拟器 -->
    <footer class="app-footer">
      <InteractionForm />
    </footer>
  </div>
</template>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px;
  background: var(--bg-surface);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
}

.brand {
  font-size: 22px;
  font-weight: 700;
  color: var(--accent);
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 15px;
  color: var(--text-secondary);
}

.refresh-timer {
  font-variant-numeric: tabular-nums;
  color: var(--text-muted);
}

.app-tabs {
  padding: 0 24px;
  background: var(--bg-surface);
  border-bottom: 1px solid var(--border-color);
}

.app-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.app-tabs :deep(.el-tabs__nav) {
  border: none !important;
}

.app-tabs :deep(.el-tabs__item) {
  border: none !important;
  color: var(--text-secondary);
  font-size: 16px;
  padding: 0 20px;
  height: 44px;
  line-height: 44px;
}

.app-tabs :deep(.el-tabs__item.is-active) {
  color: var(--accent);
  background: transparent;
  border-bottom: 2px solid var(--accent) !important;
}

.app-main {
  flex: 1;
  padding: 20px 24px;
  max-width: 1440px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}

.app-footer {
  padding: 0 24px 20px;
  max-width: 1440px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}
</style>
