<script setup>
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({ data: Object })
const chartRef = ref(null)
let chart = null

function render() {
  if (!chart || !props.data) return

  const dailyTrend = props.data.dailyTrend || []
  const byReason = props.data.byReason || []

  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: dailyTrend.map(d => d.date),
      axisLabel: { color: '#94a3b8', rotate: 30 }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#94a3b8' },
      splitLine: { lineStyle: { color: '#334155' } }
    },
    series: [{
      type: 'line',
      data: dailyTrend.map(d => d.count),
      smooth: true,
      lineStyle: { color: '#ef4444', width: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(239,68,68,0.3)' },
          { offset: 1, color: 'rgba(239,68,68,0.02)' }
        ])
      },
      itemStyle: { color: '#ef4444' }
    }],
    grid: { left: 50, right: 20, top: 20, bottom: 40 }
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
