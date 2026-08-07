/**
 * composables/useDialogAdapt.js · Element Plus 弹窗移动端/PC 尺寸自适应
 *
 * 为什么抽这个：HomePage 两个弹窗 watch 逻辑重复 90%（都是拿 dialog DOM →
 * 移动端固定宽高 + body 滚动 → PC 端 max-width + body 滚动）。
 * 抽成组合式函数后，每个弹窗组件一行调用。
 *
 * 用法：
 *   useDialogAdapt('notice-dialog', () => props.modelValue, { maxWidthPc: 720, withFooter: true })
 */
import { watch } from 'vue'

export function useDialogAdapt(
  dialogClass,
  isOpen,                 // 传 getter：() => props.modelValue 或 ref
  { maxWidthPc = 720, withFooter = true, maxHeightMobile = '88vh' } = {}
) {
  watch(isOpen, async (val) => {
    if (!val) return
    // Element Plus Teleport 到 body，等几帧确保 DOM 渲染完成
    await new Promise((resolve) => setTimeout(resolve, 200))
    const dialog = document.querySelector(`.${dialogClass}`)
    if (!dialog) return

    const isMobile = window.innerWidth < 768

    if (isMobile) {
      // 移动端：固定宽度 + 顶部留 5vh 间距 + body 弹性滚动
      dialog.style.setProperty('width', '95vw', 'important')
      dialog.style.maxWidth = '355px'
      dialog.style.maxHeight = maxHeightMobile
      dialog.style.setProperty('top', 'auto', 'important')
      dialog.style.setProperty('transform', 'none', 'important')
      const overlayDialog = document.querySelector('.el-overlay-dialog')
      if (overlayDialog) {
        overlayDialog.style.setProperty('align-items', 'flex-start', 'important')
        overlayDialog.style.setProperty('justify-content', 'center', 'important')
        overlayDialog.style.setProperty('display', 'flex', 'important')
        overlayDialog.style.setProperty('padding-top', '5vh', 'important')
      }
      fitBody(dialog, maxHeightMobile, withFooter)
    } else {
      // PC 端：max-width + body 内部滚动
      dialog.style.maxWidth = `${maxWidthPc}px`
      fitBody(dialog, '85vh', withFooter)
    }
  })
}

/** 计算 body 可用高度 = 弹窗总高 - header - footer - buffer */
async function fitBody(dialog, totalHeight, withFooter) {
  await new Promise((resolve) => setTimeout(resolve, 50))
  const headerH = dialog.querySelector('.el-dialog__header')?.getBoundingClientRect().height ?? 53
  const footerH = withFooter
    ? (dialog.querySelector('.el-dialog__footer')?.getBoundingClientRect().height ?? 49)
    : 0
  const total = parseInt(totalHeight, 10) * window.innerHeight / 100
  const maxBodyH = Math.floor(total - headerH - footerH - 8)
  const body = dialog.querySelector('.el-dialog__body')
  if (body) {
    body.style.setProperty('max-height', maxBodyH + 'px', 'important')
    body.style.overflowY = 'auto'
  }
}
