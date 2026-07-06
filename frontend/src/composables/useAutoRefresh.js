import { ref, onUnmounted } from 'vue'

export function useAutoRefresh(callback, intervalMs = 30000) {
  const secondsLeft = ref(intervalMs / 1000)
  let timer = null
  let countdownTimer = null

  function start() {
    stop()
    secondsLeft.value = intervalMs / 1000
    countdownTimer = setInterval(() => {
      secondsLeft.value--
      if (secondsLeft.value <= 0) {
        secondsLeft.value = intervalMs / 1000
        callback()
      }
    }, 1000)
  }

  function stop() {
    if (countdownTimer) clearInterval(countdownTimer)
    countdownTimer = null
  }

  function reset() {
    secondsLeft.value = intervalMs / 1000
  }

  start()
  onUnmounted(stop)

  return { secondsLeft, start, stop, reset }
}
