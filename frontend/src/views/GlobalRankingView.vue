<script setup>
import { ref, onMounted, inject, onUnmounted } from 'vue'
import { fetchGlobalRanking } from '../api/ranking'
import RankingTable from '../components/RankingTable.vue'
import TopicDetailDialog from '../components/TopicDetailDialog.vue'

const items = ref([])
const loading = ref(true)
const dialogVisible = ref(false)
const selectedTopicId = ref(null)

async function load() {
  loading.value = true
  try {
    const data = await fetchGlobalRanking(50)
    items.value = data.items || []
  } catch (e) {
    items.value = []
  } finally {
    loading.value = false
  }
}

function handleRowClick(topicId) {
  selectedTopicId.value = topicId
  dialogVisible.value = true
}

const registerRefresh = inject('registerRefresh', null)
const unregister = registerRefresh?.(load)
onUnmounted(() => unregister?.())

onMounted(load)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; align-items: center; justify-content: space-between;">
        <span style="font-weight: 600; font-size: 16px;">🌐 全站热榜</span>
        <el-tag type="info" size="small">{{ items.length }} 个话题</el-tag>
      </div>
    </template>
    <RankingTable :items="items" :loading="loading" @row-click="handleRowClick" />
  </el-card>

  <TopicDetailDialog
    v-model:visible="dialogVisible"
    :topic-id="selectedTopicId"
    @refresh="load"
  />
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
