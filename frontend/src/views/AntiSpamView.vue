<script setup>
import { ref, onMounted, inject, onUnmounted } from 'vue'
import { fetchAntiCheatStats } from '../api/analysis'
import { fetchAntiSpamReport } from '../api/interaction'
import AntiSpamTrendChart from '../components/charts/AntiSpamTrendChart.vue'
import AntiSpamReasonChart from '../components/charts/AntiSpamReasonChart.vue'

const loading = ref(true)
const antiCheat = ref({})
const report = ref({})

async function load() {
  loading.value = true
  try {
    const [ac, rp] = await Promise.all([
      fetchAntiCheatStats(7),
      fetchAntiSpamReport()
    ])
    antiCheat.value = ac || {}
    report.value = rp || {}
  } catch (e) {
    antiCheat.value = {}
    report.value = {}
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
          <div class="label">7 天拦截总数</div>
          <div class="value" style="color: var(--red);">{{ antiCheat.totalBlockedCount || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="label">今日拦截</div>
          <div class="value" style="color: var(--orange);">{{ report.totalBlockedCount || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="label">受影响话题</div>
          <div class="value">{{ report.affectedTopicCount || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="label">可疑用户</div>
          <div class="value">{{ report.suspiciousUsers?.length || 0 }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表 -->
    <el-row :gutter="16" style="margin-bottom: 20px;">
      <el-col :xs="24" :sm="14">
        <el-card shadow="never" body-style="padding: 0;">
          <template #header>
            <span style="font-weight: 600;">每日拦截趋势（近 7 天）</span>
          </template>
          <AntiSpamTrendChart :data="antiCheat" />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="10">
        <el-card shadow="never" body-style="padding: 0;">
          <template #header>
            <span style="font-weight: 600;">拦截原因分布</span>
          </template>
          <AntiSpamReasonChart :data="antiCheat" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 可疑用户表格 -->
    <el-card shadow="never">
      <template #header>
        <span style="font-weight: 600;">可疑用户 Top 10</span>
      </template>
      <el-table
        :data="report.suspiciousUsers || []"
        stripe
        :header-cell-style="{ background: 'var(--bg-elevated)', color: 'var(--text-secondary)' }"
      >
        <el-table-column type="index" label="#" width="64" align="center" />
        <el-table-column prop="userId" label="用户 ID" />
        <el-table-column prop="blockCount" label="拦截次数" width="120" align="right">
          <template #default="{ row }">
            <span style="color: var(--red); font-weight: 600;">{{ row.blockCount }}</span>
          </template>
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
