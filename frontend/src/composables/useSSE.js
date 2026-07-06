import { ref, onUnmounted } from 'vue'

export function useSSE(url = '/api/notifications/rankings/stream') {
  const connected = ref(false)
  const subscriberCount = ref(0)
  let eventSource = null
  let retryTimeout = null
  let retryDelay = 1000

  const handlers = {
    onConnected: null,
    onRankingUpdated: null,
    onTopNEntered: null
  }

  function connect() {
    if (eventSource) eventSource.close()

    eventSource = new EventSource(url)

    eventSource.addEventListener('connected', (e) => {
      const data = JSON.parse(e.data)
      connected.value = true
      subscriberCount.value = data.subscriberCount || 0
      retryDelay = 1000
      handlers.onConnected?.(data)
    })

    eventSource.addEventListener('ranking-updated', (e) => {
      const data = JSON.parse(e.data)
      handlers.onRankingUpdated?.(data)
    })

    eventSource.addEventListener('top-n-entered', (e) => {
      const data = JSON.parse(e.data)
      handlers.onTopNEntered?.(data)
    })

    eventSource.onerror = () => {
      connected.value = false
      eventSource.close()
      retryTimeout = setTimeout(() => {
        retryDelay = Math.min(retryDelay * 2, 30000)
        connect()
      }, retryDelay)
    }
  }

  function on(event, handler) {
    handlers[event] = handler
  }

  function disconnect() {
    if (retryTimeout) clearTimeout(retryTimeout)
    if (eventSource) eventSource.close()
    connected.value = false
  }

  connect()

  onUnmounted(disconnect)

  return { connected, subscriberCount, on, disconnect }
}
