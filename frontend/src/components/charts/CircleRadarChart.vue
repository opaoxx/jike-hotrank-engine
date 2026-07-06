<script setup>
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({ data: Object })
const chartRef = ref(null)
let chart = null

function render() {
  if (!chart || !props.data?.items?.length) return
  const items = props.data.items.slice(0, 6)

  const maxTopics = Math.max(...items.map(i => i.topicCount || 0), 1)
  const maxScore = Math.max(...items.map(i => i.avgScore || 0), 1)
  const maxInteractions = Math.max(...items.map(i => i.interactionCount || 0), 1)

  chart.setOption({
    tooltip: {},
    radar: {
      indicator: [
        { name: '话题数', max: maxTopics * 1.2 },
        { name: '平均热度', max: maxScore * 1.2 },
        { name: '互动数', max: maxInteractions * 1.2 }
      ],
      axisName: { color: '#94a3b8' },
      splitArea: { areaStyle: { color: ['rgba(59,130,246,0.05)', 'rgba(59,130,246,0.1)'] } },
      splitLine: { lineStyle: { color: '#334155' } },
      axisLine: { lineStyle: { color: '#334155' } }
    },
    series: [{
      type: 'radar',
      data: items.map((item, i) => ({
        value: [item.topicCount || 0, item.avgScore || 0, item.interactionCount || 0],
        name: item.circleName,
        areaStyle: { opacity: 0.15 }
      }))
    }],
    legend: {
      bottom: 0,
      textStyle: { color: '#94a3b8' }
    }
  })
}

onMounted(() => {
  chart = echarts.init(chartRef.value)
  render()
})

watch(() => props.data, render, { deep: true })
</script>

<template>
  <div ref="chartRef" class="chart-container"></div>
</template>
