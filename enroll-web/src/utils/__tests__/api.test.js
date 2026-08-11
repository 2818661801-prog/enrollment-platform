// @vitest-environment jsdom
import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../../stores/auth.js'

describe('request() token 注入', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  it('有 token 时自动带 Authorization header', async () => {
    const auth = useAuthStore()
    auth.login('test-token-123', '***REMOVED***')

    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ code: 200, data: [] }),
    })
    vi.stubGlobal('fetch', mockFetch)

    // 动态 import 确保拿到最新模块
    const { fetchMyApplicationsMe } = await import('../api.js')
    await fetchMyApplicationsMe()

    const [url, opts] = mockFetch.mock.calls[0]
    expect(opts.headers.Authorization).toBe('Bearer test-token-123')
  })

  it('无 token 时不带 Authorization header', async () => {
    // 不登录，store.token 为空
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ code: 200, data: [] }),
    })
    vi.stubGlobal('fetch', mockFetch)

    const { fetchClasses } = await import('../api.js')
    await fetchClasses()

    const [url, opts] = mockFetch.mock.calls[0]
    expect(opts.headers.Authorization).toBeUndefined()
  })

  it('非 2xx 响应抛错含 status', async () => {
    const mockFetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
    })
    vi.stubGlobal('fetch', mockFetch)

    const { fetchClasses } = await import('../api.js')
    await expect(fetchClasses()).rejects.toThrow('HTTP 500')
  })

  it('非 2xx 响应 error 对象含 status 属性', async () => {
    const mockFetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 403,
      statusText: 'Forbidden',
    })
    vi.stubGlobal('fetch', mockFetch)

    const { fetchClasses } = await import('../api.js')
    try {
      await fetchClasses()
      expect.unreachable('should have thrown')
    } catch (err) {
      expect(err.status).toBe(403)
    }
  })

  it('网络错误（fetch reject）→ 抛 TypeError', async () => {
    const mockFetch = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'))
    vi.stubGlobal('fetch', mockFetch)

    const { fetchClasses } = await import('../api.js')
    await expect(fetchClasses()).rejects.toThrow('Failed to fetch')
  })

  it('自定义 header 合并（不覆盖 Content-Type）', async () => {
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ code: 200, data: {} }),
    })
    vi.stubGlobal('fetch', mockFetch)

    // 动态 import 后直接调 request
    const apiModule = await import('../api.js')
    // request 不是 export，但 submitApplicationAPI 会传 method+body
    // 测试 Content-Type 是否正确
    await apiModule.submitApplicationAPI({ name: 'test' })

    const [url, opts] = mockFetch.mock.calls[0]
    expect(opts.headers['Content-Type']).toBe('application/json')
    expect(opts.method).toBe('POST')
  })

  it('404 响应抛错含 status', async () => {
    const mockFetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 404,
      statusText: 'Not Found',
    })
    vi.stubGlobal('fetch', mockFetch)

    const { fetchClass } = await import('../api.js')
    await expect(fetchClass(99999)).rejects.toThrow('HTTP 404')
  })
})
