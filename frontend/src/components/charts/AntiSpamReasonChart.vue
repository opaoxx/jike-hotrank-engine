<script setup>
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({ data: Object })
const chartRef = ref(null)
let chart = null

const reasonLabels = {
  rate_limit: '\u9891\u7387\u8d85\u9650',
  frequency_limit: '\u9891\u7387\u8d85\u9650',
  device_penalty: '\u8bbe\u5907\u964d\u6743',
  device_fingerprint_penalty: '\u8bbe\u5907\u6307\u7eb9\u964d\u6743',
  anomaly_surge: '\u5f02\u5e38\u7a81\u589e'
}

const colors = ['#ef4444', '#f97316', '#eab308']

function normalizeReasons(value) {
  if (Array.isArray(value)) {
    return value.map(item => ({
      reason: item.reason,
      count: Number(item.count || 0)
    }))
  }

  if (value && typeof value === 'object') {
    return Object.entries(value).map(([reason, count]) => ({
      reason,
      count: Number(count || 0)
    }))
  }

  return []
}

function render() {
  if (!chart || !props.data) return

  const byReason = normalizeReasons(props.data.byReason)

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
      data: byReason.map((r, i) => ({
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
