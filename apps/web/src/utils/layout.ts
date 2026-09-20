import { useAppStoreWithOut } from '@/store/modules/app'
import { isFeishuClient } from './environment'

// 飞书客户端推荐的布局类型（与 environment.ts 中的 FEISHU_LAYOUT 保持一致）
const FEISHU_LAYOUT = 'top'

/**
 * 根据当前环境自动设置合适的布局
 */
export function setupAutoLayout() {
  const appStore = useAppStoreWithOut()
  
  // 检测是否在飞书客户端内
  if (isFeishuClient()) {
    // 在飞书客户端内使用配置的布局
    if (appStore.getLayout !== FEISHU_LAYOUT) {
      appStore.setLayout(FEISHU_LAYOUT)
    }
  }
}

/**
 * 监听环境变化并相应调整布局
 * 可以用于处理从普通浏览器切换到飞书内置浏览器的情况
 */
export function watchEnvironmentForLayout() {
  // 定期检查环境变化（例如，用户可能在飞书内打开链接后复制链接到外部浏览器）
  setInterval(() => {
    setupAutoLayout()
  }, 5000) // 每5秒检查一次
}