<script setup>
import { ref, onMounted, watch, inject, onUnmounted } from 'vue'
import { fetchCircleRanking } from '../api/ranking'
import { fetchCircleActivity } from '../api/analysis'
import { useAppStore } from '../stores/app'
import RankingTable from '../components/RankingTable.vue'
import TopicDetailDialog from '../components/TopicDetailDialog.vue'

const store = useAppStore()
const items = ref([])
const loading = ref(true)
const selectedCircle = ref(null)
const dialogVisible = ref(false)
const selectedTopicId = ref(null)

async function loadCircles() {
  if (store.circleList.length) return
  try {
    const data = await fetchCircleActivity()
    store.setCircleList(data.items || [])
  } catch (e) { /* ignore */ }
}

async function loadRanking() {
  if (!selectedCircle.value) return
  loading.value = true
  try {
    const data = await fetchCircleRanking(selectedCircle.value, 20)
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
const unregister = registerRefresh?.(loadRanking)
onUnmounted(() => unregister?.())

watch(selectedCircle, loadRanking)

onMounted(async () => {
  await loadCircles()
  if (store.circleList.length) {
    selectedCircle.value = store.circleList[0].circleId
  }
})

defineExpose({ load: loadRanking })
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; align-items: center; justify-content: space-between;">
        <span style="font-weight: 600; font-size: 16px;">⭕ 圈子热榜</span>
        <el-select
          v-model="selectedCircle"
          placeholder="选择圈子"
          style="width: 200px;"
        >
          <el-option
            v-for="c in store.circleList"
            :key="c.circleId"
            :label="c.circleName"
            :value="c.circleId"
          />
        </el-select>
      </div>
    </template>
    <RankingTable :items="items" :loading="loading" @row-click="handleRowClick" />
  </el-card>

  <TopicDetailDialog
    v-model:visible="dialogVisible"
    :topic-id="selectedTopicId"
    @refresh="loadRanking"
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
