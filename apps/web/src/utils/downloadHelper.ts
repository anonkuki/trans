/**
 * 判断字符串是否为IP地址
 * @param str 待判断的字符串
 * @returns 是否为IP地址
 */
export const isIpAddress = (str: string): boolean => {
  // IPv4正则表达式
  const ipv4Regex = /^(\d{1,3}\.){3}\d{1,3}$/

  if (!ipv4Regex.test(str)) {
    return false
  }

  // 检查每个数字段是否在0-255范围内
  const parts = str.split('.')
  for (let i = 0; i < parts.length; i++) {
    const num = parseInt(parts[i], 10)
    if (num < 0 || num > 255) {
      return false
    }
  }

  return true
}

/**
 * 获取当前浏览器的主机名（hostname）
 * @returns 当前主机名
 */
export const getCurrentHostname = (): string => {
  return window.location.hostname
}

/**
 * 判断当前访问地址是否为IP地址
 * @returns 是否为IP地址
 */
export const isCurrentHostIp = (): boolean => {
  const hostname = getCurrentHostname()
  return isIpAddress(hostname)
}

/**
 * 将URL中的域名替换为指定的IP地址
 * @param url 原始URL
 * @param ip IP地址
 * @returns 替换后的URL
 */
export const replaceUrlDomainWithIp = (url: string, ip: string): string => {
  try {
    const urlObj = new URL(url)
    // 保留端口号（如果有的话）
    const port = urlObj.port ? `:${urlObj.port}` : ''
    urlObj.host = `${ip}${port}`
    return urlObj.toString()
  } catch (error) {
    console.error('URL解析失败:', error)
    return url
  }
}

/**
 * 智能处理下载URL：如果当前访问的是IP地址，则将下载链接的域名也替换为该IP
 * @param url 原始下载URL
 * @returns 处理后的URL
 */
export const processDownloadUrl = (url: string): string => {
  if (!url) return url

  // 如果当前访问的是IP地址
  if (isCurrentHostIp()) {
    const currentIp = getCurrentHostname()
    return replaceUrlDomainWithIp(url, currentIp)
  }

  // 否则返回原URL
  return url
}
