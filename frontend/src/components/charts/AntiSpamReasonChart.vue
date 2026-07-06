<script setup>
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({ data: Object })
const chartRef = ref(null)
let chart = null

const reasonLabels = {
  frequency_limit: '频率超限',
  device_penalty: '设备降权',
  anomaly_surge: '异常突增'
}

const colors = ['#ef4444', '#f97316', '#eab308']

function render() {
  if (!chart || !props.data?.byReason) return

  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: {
      bottom: 0,
      textStyle: { color: '#94a3b8' }
    },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      center: ['50%', '42%'],
      itemStyle: { borderColor: '#1e293b', borderWidth: 2 },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold' }
      },
      data: props.data.byReason.map((r, i) => ({
        name: reasonLabels[r.reason] || r.reason,
        value: r.count,
        itemStyle: { color: colors[i % colors.length] }
      }))
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
