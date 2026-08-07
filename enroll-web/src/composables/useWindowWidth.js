/**
 * composables/useWindowWidth.js · 响应式窗口宽度
 *
 * 为什么抽这个：多个组件各自写了三件套（windowWidth ref + onResize + resize 监听），
 * 复制粘贴 N 次。抽成组合式函数，一处实现，多处复用。
 *
 * 用法：
 *   const windowWidth = useWindowWidth()   // 响应式数字，窗口 resize 时自动更新
 */
import { ref, onMounted, onUnmounted } from 'vue'

export function useWindowWidth() {
  const windowWidth = ref(window.innerWidth)
  function onResize() {
    windowWidth.value = window.innerWidth
  }
  onMounted(() => window.addEventListener('resize', onResize))
  onUnmounted(() => window.removeEventListener('resize', onResize))
  return windowWidth
}
