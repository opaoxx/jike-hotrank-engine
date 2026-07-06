<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  showCircle: { type: Boolean, default: true }
})

const emit = defineEmits(['row-click'])

function rankClass(rank) {
  if (rank === 1) return 'rank-1'
  if (rank === 2) return 'rank-2'
  if (rank === 3) return 'rank-3'
  return ''
}

const columns = computed(() => {
  const cols = [
    { prop: 'rank', label: '#', width: 64, align: 'center' },
    { prop: 'title', label: '话题', minWidth: 200 },
  ]
  if (props.showCircle) {
    cols.push({ prop: 'circleName', label: '圈子', width: 120 })
  }
  cols.push(
    { prop: 'score', label: '热度', width: 120, align: 'right' },
    { prop: 'interactionCount', label: '互动数', width: 100, align: 'right' }
  )
  return cols
})

function handleRowClick(row) {
  emit('row-click', row.topicId)
}

function formatScore(score) {
  return (score || 0).toFixed(1)
}

function formatCount(count) {
  return (count || 0).toLocaleString()
}
</script>

<template>
  <el-table
    :data="items"
    v-loading="loading"
    stripe
    highlight-current-row
    @row-click="handleRowClick"
    style="width: 100%; cursor: pointer;"
    :header-cell-style="{ background: 'var(--bg-elevated)', color: 'var(--text-secondary)' }"
  >
    <el-table-column prop="rank" label="#" width="64" align="center">
      <template #default="{ row }">
        <span class="rank-badge" :class="rankClass(row.rank)">{{ row.rank }}</span>
      </template>
    </el-table-column>

    <el-table-column prop="title" label="话题" min-width="200" show-overflow-tooltip />

    <el-table-column v-if="showCircle" prop="circleName" label="圈子" width="120" show-overflow-tooltip />

    <el-table-column prop="score" label="热度" width="120" align="right">
      <template #default="{ row }">
        <span style="font-variant-numeric: tabular-nums; color: var(--accent);">
          {{ formatScore(row.score) }}
        </span>
      </template>
    </el-table-column>

    <el-table-column prop="interactionCount" label="互动数" width="100" align="right">
      <template #default="{ row }">
        {{ formatCount(row.interactionCount) }}
      </template>
    </el-table-column>
  </el-table>
</template>
