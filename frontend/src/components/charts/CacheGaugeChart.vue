<script setup>
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({ data: Object })
const chartRef = ref(null)
let chart = null

function render() {
  if (!chart || !props.data) return

  const hitRate = ((props.data.hitRate || 0) * 100).toFixed(1)
  const hits = props.data.hits ?? props.data.hitCount ?? 0
  const misses = props.data.misses ?? props.data.missCount ?? 0

  chart.setOption({
    series: [{
      type: 'gauge',
      startAngle: 200,
      endAngle: -20,
      min: 0,
      max: 100,
      pointer: { show: true, length: '70%', width: 4, itemStyle: { color: '#3b82f6' } },
      axisLine: {
        lineStyle: {
          width: 20,
          color: [
            [0.6, '#ef4444'],
            [0.8, '#eab308'],
            [1, '#22c55e']
          ]
        }
      },
      axisTick: { show: false },
      splitLine: { length: 12, lineStyle: { color: 'auto' } },
      axisLabel: { color: '#94a3b8', distance: 28, fontSize: 12 },
      detail: {
        valueAnimation: true,
        formatter: '{value}%',
        color: '#e2e8f0',
        fontSize: 24,
        offsetCenter: [0, '60%']
      },
      title: {
        offsetCenter: [0, '85%'],
        color: '#94a3b8',
        fontSize: 13
      },
      data: [{ value: hitRate, name: `\u547d\u4e2d ${hits} / \u672a\u547d\u4e2d ${misses}` }]
    }]
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
