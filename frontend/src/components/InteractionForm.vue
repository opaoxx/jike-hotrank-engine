<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { submitInteraction } from '../api/interaction'

const form = ref({
  topicId: 1,
  userId: 9001,
  interactionType: 1,
  deviceFingerprint: '',
  ipAddress: ''
})

const loading = ref(false)
const collapsed = ref(true)

const typeOptions = [
  { value: 1, label: '👍 点赞', weight: '×1' },
  { value: 2, label: '🔖 收藏', weight: '×2' },
  { value: 3, label: '📤 转发', weight: '×3' },
  { value: 5, label: '💬 评论', weight: '×5' }
]

async function handleSubmit() {
  loading.value = true
  try {
    const body = { ...form.value }
    if (!body.deviceFingerprint) delete body.deviceFingerprint
    if (!body.ipAddress) delete body.ipAddress
    const result = await submitInteraction(body)
    ElMessage.success(`互动已记录 ID=${result.id}，权重=${result.weightMultiplier}`)
  } catch (e) {
    // error already handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-collapse v-model="collapsed">
    <el-collapse-item title="🧪 互动模拟器" name="1">
      <el-form :model="form" inline label-width="80px" @submit.prevent="handleSubmit">
        <el-form-item label="话题 ID">
          <el-input-number v-model="form.topicId" :min="1" :controls="false" style="width: 120px" />
        </el-form-item>
        <el-form-item label="用户 ID">
          <el-input-number v-model="form.userId" :min="1" :controls="false" style="width: 120px" />
        </el-form-item>
        <el-form-item label="互动类型">
          <el-select v-model="form.interactionType" style="width: 140px">
            <el-option
              v-for="opt in typeOptions"
              :key="opt.value"
              :label="`${opt.label} (${opt.weight})`"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="设备指纹">
          <el-input v-model="form.deviceFingerprint" placeholder="可选" style="width: 160px" />
        </el-form-item>
        <el-form-item label="IP 地址">
          <el-input v-model="form.ipAddress" placeholder="可选" style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit">提交互动</el-button>
        </el-form-item>
      </el-form>
    </el-collapse-item>
  </el-collapse>
</template>

<style scoped>
.el-collapse {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-surface);
  overflow: hidden;
}

:deep(.el-collapse-item__header) {
  padding: 0 20px;
  background: var(--bg-surface);
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 600;
  border-bottom: 1px solid var(--border-color);
}

:deep(.el-collapse-item__content) {
  padding: 16px 20px;
  background: var(--bg-surface);
}
</style>
