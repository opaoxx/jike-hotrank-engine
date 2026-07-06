<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchTopicDetail, blockTopic, unblockTopic } from '../api/topic'

const props = defineProps({
  visible: Boolean,
  topicId: Number
})

const emit = defineEmits(['update:visible', 'refresh'])

const topic = ref(null)
const loading = ref(false)

const statusMap = {
  0: { label: '已屏蔽', type: 'danger' },
  1: { label: '正常', type: 'success' },
  2: { label: '待审核', type: 'warning' }
}

watch(() => props.topicId, async (id) => {
  if (!id) return
  loading.value = true
  try {
    topic.value = await fetchTopicDetail(id)
  } catch (e) {
    topic.value = null
  } finally {
    loading.value = false
  }
})

async function handleBlock() {
  try {
    await blockTopic(props.topicId)
    ElMessage.success('话题已屏蔽')
    emit('update:visible', false)
    emit('refresh')
  } catch (e) { /* handled */ }
}

async function handleUnblock() {
  try {
    await unblockTopic(props.topicId)
    ElMessage.success('话题已取消屏蔽')
    emit('update:visible', false)
    emit('refresh')
  } catch (e) { /* handled */ }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    title="话题详情"
    width="640px"
    :close-on-click-modal="true"
  >
    <div v-loading="loading" style="min-height: 200px;">
      <template v-if="topic">
        <h3 style="margin: 0 0 16px; font-size: 20px;">{{ topic.title }}</h3>

        <el-descriptions :column="3" border>
          <el-descriptions-item label="状态">
            <el-tag :type="statusMap[topic.status]?.type" effect="dark">
              {{ statusMap[topic.status]?.label || '未知' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="话题 ID">{{ topic.id }}</el-descriptions-item>
          <el-descriptions-item label="圈子 ID">{{ topic.circleId }}</el-descriptions-item>
          <el-descriptions-item label="作者 ID">{{ topic.authorId }}</el-descriptions-item>
          <el-descriptions-item label="当前热度">
            <span style="color: var(--green); font-weight: 600;">
              {{ (topic.currentScore || 0).toFixed(4) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="互动数">
            {{ (topic.interactionCount || 0).toLocaleString() }}
          </el-descriptions-item>
          <el-descriptions-item label="发布时间" :span="3">
            {{ topic.publishTime?.replace('T', ' ') || '—' }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="topic.content" style="margin-top: 16px; color: var(--text-secondary); line-height: 1.8;">
          {{ topic.content }}
        </div>
      </template>
    </div>

    <template #footer>
      <el-button @click="emit('update:visible', false)">关闭</el-button>
      <template v-if="topic">
        <el-button
          v-if="topic.status === 0"
          type="success"
          @click="handleUnblock"
        >
          取消屏蔽
        </el-button>
        <el-button
          v-else
          type="danger"
          @click="handleBlock"
        >
          屏蔽话题
        </el-button>
      </template>
    </template>
  </el-dialog>
</template>
