<script setup>
import { ref, onMounted, inject, onUnmounted } from 'vue'
import { fetchOverview } from '../api/analysis'
import { useAppStore } from '../stores/app'
import HeatDistChart from '../components/charts/HeatDistChart.vue'
import InteractionPieChart from '../components/charts/InteractionPieChart.vue'
import CircleRadarChart from '../components/charts/CircleRadarChart.vue'
import CacheGaugeChart from '../components/charts/CacheGaugeChart.vue'

const store = useAppStore()
const loading = ref(true)
const overview = ref({})

function cacheHits(stats) {
  return stats?.hits ?? stats?.hitCount ?? 0
}

function cacheMisses(stats) {
  return stats?.misses ?? stats?.missCount ?? 0
}

async function load() {
  loading.value = true
  try {
    overview.value = await fetchOverview()
    if (overview.value.circleActivity?.items) {
      store.setCircleList(overview.value.circleActivity.items)
    }
  } catch (e) {
    overview.value = {}
  } finally {
    loading.value = false
  }
}

const registerRefresh = inject('registerRefresh', null)
const unregister = registerRefresh?.(load)
onUnmounted(() => unregister?.())

onMounted(load)
</script>

<template>
  <div v-loading="loading" style="min-height: 400px;">
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 20px;">
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="label">话题总数</div>
          <div class="value">{{ overview.heatDistribution?.topicCount || 0 }}</div>
          <div class="sub">最高热度：{{ (overview.heatDistribution?.maxScore || 0).toFixed(1) }}</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="label">互动次数（{{ overview.interactionStats?.hours || 24 }}h）</div>
          <div class="value">{{ (overview.interactionStats?.total || 0).toLocaleString() }}</div>
          <div class="sub">4 类互动事件总计</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="label">平均热度</div>
          <div class="value">{{ (overview.heatDistribution?.avgScore || 0).toFixed(1) }}</div>
          <div class="sub">中位数：{{ (overview.heatDistribution?.medianScore || 0).toFixed(1) }}</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="label">缓存命中率</div>
          <div class="value" style="color: var(--green);">
            {{ ((overview.cacheStats?.hitRate || 0) * 100).toFixed(1) }}%
          </div>
          <div class="sub">命中 {{ cacheHits(overview.cacheStats) }} / 未命中 {{ cacheMisses(overview.cacheStats) }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表 2×2 -->
    <el-row :gutter="16" style="margin-bottom: 20px;">
      <el-col :xs="24" :sm="12">
        <el-card shadow="never" body-style="padding: 0;">
          <template #header>
            <span style="font-weight: 600;">热度分布</span>
          </template>
          <HeatDistChart :data="overview.heatDistribution" />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-card shadow="never" body-style="padding: 0;">
          <template #header>
            <span style="font-weight: 600;">互动类型分布</span>
          </template>
          <InteractionPieChart :data="overview.interactionStats" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-bottom: 20px;">
      <el-col :xs="24" :sm="12">
        <el-card shadow="never" body-style="padding: 0;">
          <template #header>
            <span style="font-weight: 600;">圈子多维对比</span>
          </template>
          <CircleRadarChart :data="overview.circleActivity" />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-card shadow="never" body-style="padding: 0;">
          <template #header>
            <span style="font-weight: 600;">缓存命中率</span>
          </template>
          <CacheGaugeChart :data="overview.cacheStats" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 圈子活跃度表格 -->
    <el-card shadow="never">
      <template #header>
        <span style="font-weight: 600;">圈子活跃度</span>
      </template>
      <el-table
        :data="overview.circleActivity?.items || []"
        stripe
        :header-cell-style="{ background: 'var(--bg-elevated)', color: 'var(--text-secondary)' }"
      >
        <el-table-column prop="rank" label="#" width="64" align="center">
          <template #default="{ row }">
            <span class="rank-badge" :class="row.rank <= 3 ? 'rank-' + row.rank : ''">
              {{ row.rank }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="circleName" label="圈子" />
        <el-table-column prop="topicCount" label="话题数" width="100" align="right" />
        <el-table-column prop="avgScore" label="平均热度" width="120" align="right">
          <template #default="{ row }">{{ (row.avgScore || 0).toFixed(1) }}</template>
        </el-table-column>
        <el-table-column prop="interactionCount" label="互动数" width="120" align="right">
          <template #default="{ row }">{{ (row.interactionCount || 0).toLocaleString() }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
:deep(.el-card) {
  background: var(--bg-surface);
  border-color: var(--border-color);
}

:deep(.el-card__header) {
  padding: 12px 20px;
  border-bottom-color: var(--border-color);
  color: var(--text-primary);
}
</style>
