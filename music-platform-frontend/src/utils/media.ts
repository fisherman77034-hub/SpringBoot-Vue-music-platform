/**
 * 后端根地址（不含路径），用于拼接 /api、/files 等。
 * - 必配：前后端不同域名/端口且非 Vite 代理时，设置 VITE_API_BASE_URL（如 http://192.168.1.3:8080）
 * - 开发 (npm run dev)：默认 当前浏览器访问前端的「主机名」+ 后端端口（默认 8080），避免写死 127.0.0.1 导致用手机/局域网 IP 打开页面时媒体仍请求本机环回而失败
 * - 生产构建：未配置时返回空字符串，使用相对路径 /files/...（需网关同域反代，或用 vite preview 已配置的 proxy）
 */
export function getBackendOrigin(): string {
  const env = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim()
  if (env) return env.replace(/\/$/, '')

  if (typeof window === 'undefined') return ''

  const port =
    (import.meta.env.VITE_BACKEND_PORT as string | undefined)?.trim() ||
    (import.meta.env.VITE_API_PORT as string | undefined)?.trim() ||
    '8080'

  if (import.meta.env.DEV) {
    const { protocol, hostname } = window.location
    return `${protocol}//${hostname}:${port}`
  }

  return ''
}

export function resolveMediaUrl(path: string | null | undefined): string {
  if (path == null || path === '') return ''
  if (/^https?:\/\//i.test(path)) return path
  const p = path.startsWith('/') ? path : `/${path}`
  const base = getBackendOrigin()
  return base ? `${base}${p}` : p
}
