import { readFileSync } from 'fs'
import { request, type RequestOptions } from 'https'
import { request as httpRequest } from 'http'
import type { SyncConfig, SyncRequest, SyncResponse, SyncStatus, SyncState } from '../shared/types'
import type { TaskStore } from './task-store'
import type { ProjectStore } from './project-store'
import { SyncConfigStore } from './sync-config'

const SYNC_INTERVAL_MS = 60_000
const DEBOUNCE_MS = 3_000

export interface RegisterResult {
  token: string
  user: { id: string; name: string }
}

type StatusListener = (status: SyncStatus) => void

function httpRequestJson<T>(url: URL, opts: { token?: string; certPath?: string; body?: unknown }): Promise<T> {
  const isHttps = url.protocol === 'https:'
  const payload = opts.body === undefined ? null : JSON.stringify(opts.body)

  let ca: Buffer | undefined
  let rejectUnauthorized = true
  if (isHttps && opts.certPath) {
    try {
      ca = readFileSync(opts.certPath)
    } catch {
      return Promise.reject(new Error(`无法读取证书文件: ${opts.certPath}`))
    }
  } else if (isHttps && !opts.certPath) {
    // 没有自签证书时走系统 CA（域名 + 公网证书场景）
    rejectUnauthorized = true
  }

  const baseOptions: RequestOptions = {
    method: 'POST',
    hostname: url.hostname,
    port: url.port || (isHttps ? 443 : 80),
    path: `${url.pathname}${url.search}`,
    headers: {
      'Content-Type': 'application/json',
      ...(opts.token ? { Authorization: `Bearer ${opts.token}` } : {})
    },
    ...(isHttps ? { rejectUnauthorized, ca } : {})
  }

  const reqFn = isHttps ? request : httpRequest

  return new Promise<T>((resolve, reject) => {
    const req = reqFn(baseOptions as RequestOptions, (res) => {
      const chunks: Buffer[] = []
      res.on('data', (c) => chunks.push(c as Buffer))
      res.on('end', () => {
        const text = Buffer.concat(chunks).toString('utf-8')
        let data: unknown = null
        try {
          data = text ? JSON.parse(text) : null
        } catch {
          // keep null
        }
        if (res.statusCode !== undefined && res.statusCode >= 200 && res.statusCode < 300) {
          resolve(data as T)
        } else {
          const detail =
            data && typeof data === 'object' && 'detail' in (data as object)
              ? String((data as { detail: unknown }).detail)
              : text
          reject(new Error(`HTTP ${res.statusCode}: ${detail}`))
        }
      })
    })
    req.on('error', (err) => reject(err))
    if (payload) req.write(payload)
    req.end()
  })
}

export class SyncService {
  private config: SyncConfig
  private status: SyncStatus = { state: 'idle', lastSyncAt: null, message: '' }
  private listeners = new Set<StatusListener>()
  private timer: NodeJS.Timeout | null = null
  private debounceTimer: NodeJS.Timeout | null = null
  private syncing = false

  constructor(
    private readonly taskStore: TaskStore,
    private readonly projectStore: ProjectStore,
    private readonly configStore: SyncConfigStore
  ) {
    this.config = configStore.get()
  }

  start(): void {
    if (this.timer) return
    this.timer = setInterval(() => {
      void this.sync()
    }, SYNC_INTERVAL_MS)
    if (this.configStore.isConfigured()) {
      void this.sync()
    }
  }

  stop(): void {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer)
      this.debounceTimer = null
    }
  }

  onStatus(listener: StatusListener): () => void {
    this.listeners.add(listener)
    listener(this.status)
    return () => this.listeners.delete(listener)
  }

  getStatus(): SyncStatus {
    return { ...this.status }
  }

  markDirty(): void {
    if (!this.configStore.isConfigured()) return
    if (this.debounceTimer) clearTimeout(this.debounceTimer)
    this.debounceTimer = setTimeout(() => {
      void this.sync()
    }, DEBOUNCE_MS)
  }

  getConfig(): SyncConfig {
    return this.configStore.get()
  }

  setConfig(config: SyncConfig): SyncConfig {
    this.config = this.configStore.set(config)
    this.updateStatus('idle', '')
    this.markDirty()
    return this.getConfig()
  }

  async registerUser(name: string): Promise<RegisterResult> {
    const url = this.parseUrl()
    if (!url) throw new Error('请先填写服务器地址')
    return httpRequestJson<RegisterResult>(url, {
      certPath: this.config.certPath,
      body: { name }
    })
  }

  async sync(): Promise<SyncStatus> {
    if (this.syncing) return this.getStatus()
    if (!this.configStore.isConfigured()) {
      this.updateStatus('idle', '')
      return this.getStatus()
    }

    const url = this.parseUrl()
    if (!url) {
      this.updateStatus('error', '服务器地址无效')
      return this.getStatus()
    }

    this.syncing = true
    this.updateStatus('syncing', '')
    try {
      const body: SyncRequest = {
        tasks: this.taskStore.all(),
        projects: this.projectStore.all(),
        deleted: [...this.taskStore.tombstones(), ...this.projectStore.tombstones()]
      }
      const resp = await httpRequestJson<SyncResponse>(url, {
        token: this.config.token,
        certPath: this.config.certPath,
        body
      })
      this.taskStore.applyRemote(resp)
      this.projectStore.applyRemote(resp)
      this.updateStatus('ok', '')
      return this.getStatus()
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      this.updateStatus('error', message)
      return this.getStatus()
    } finally {
      this.syncing = false
    }
  }

  private parseUrl(): URL | null {
    const raw = this.config.serverUrl.trim()
    if (!raw) return null
    try {
      const url = new URL(raw)
      if (url.protocol !== 'https:' && url.protocol !== 'http:') return null
      return url
    } catch {
      return null
    }
  }

  private updateStatus(state: SyncState, message: string): void {
    const lastSyncAt = state === 'ok' ? Date.now() : this.status.lastSyncAt
    this.status = { state, lastSyncAt, message }
    for (const l of this.listeners) l({ ...this.status })
  }
}
