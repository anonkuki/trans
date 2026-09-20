/**
 * 飞书客户端推荐的布局类型
 * 可选值: 'classic' | 'topLeft' | 'top' | 'cutMenu'
 */
const FEISHU_LAYOUT = 'top'

/**
 * 检测是否在飞书客户端内打开
 * @returns {boolean} 是否在飞书客户端内
 */
export function isFeishuClient(): boolean {
  // 检查用户代理字符串是否包含飞书相关标识
  const ua = navigator.userAgent.toLowerCase()
  
  // 飞书桌面客户端和移动端通常包含以下标识
  return ua.includes('lark') || 
         ua.includes('feishu') || 
         ua.includes('bytedance') ||
         ua.includes('bytesec') || // 飞书安全浏览器
         // 检查是否有飞书JSAPI对象
         typeof (window as any).Lark !== 'undefined' ||
         typeof (window as any).tt !== 'undefined' || // 头条/字节系应用
         typeof (window as any).LarkWebJsBridge !== 'undefined' // 飞书Web JSBridge
}

/**
 * 获取适合当前环境的默认布局
 * @returns {string} 推荐的布局类型
 */
export function getDefaultLayoutForEnvironment(): string {
  // 如果在飞书客户端内，使用配置的布局
  if (isFeishuClient()) {
    return FEISHU_LAYOUT
  }
  
  // 否则使用默认的 'classic' 布局
  return 'classic'
}