import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchCacheStats } from '../api/interaction'

export const useAppStore = defineStore('app', () => {
  const cacheStats = ref({
    size: 0,
    hits: 0,
    misses: 0,
    nullHits: 0,
    hitRate: 0
  })

  const circleList = ref([])

  async function loadCacheStats() {
    try {
      cacheStats.value = await fetchCacheStats()
    } catch (e) {
      /* ignore */
    }
  }

  function setCircleList(list) {
    circleList.value = list
  }

  return { cacheStats, circleList, loadCacheStats, setCircleList }
})
