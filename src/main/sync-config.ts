import { app } from 'electron'
import { join } from 'path'
import { existsSync, readFileSync, writeFileSync } from 'fs'
import type { SyncConfig } from '../shared/types'

const EMPTY: SyncConfig = { serverUrl: '', token: '', certPath: '', userName: '' }

export class SyncConfigStore {
  private readonly file: string
  private config: SyncConfig

  constructor() {
    this.file = join(app.getPath('userData'), 'sync-config.json')
    this.config = this.load()
  }

  private load(): SyncConfig {
    try {
      if (existsSync(this.file)) {
        const parsed = JSON.parse(readFileSync(this.file, 'utf-8'))
        return { ...EMPTY, ...parsed }
      }
    } catch {
      // fall through
    }
    return { ...EMPTY }
  }

  private save(): void {
    writeFileSync(this.file, JSON.stringify(this.config, null, 2), 'utf-8')
  }

  get(): SyncConfig {
    return { ...this.config }
  }

  set(config: SyncConfig): SyncConfig {
    this.config = { ...EMPTY, ...config }
    this.save()
    return this.get()
  }

  isConfigured(): boolean {
    return Boolean(this.config.serverUrl && this.config.token)
  }
}
