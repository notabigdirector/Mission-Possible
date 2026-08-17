import { app } from 'electron'
import { dirname, join } from 'path'
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'fs'
import { randomUUID } from 'crypto'
import type { Project, ProjectInput, ProjectUpdate, SyncResponse, SyncTombstone } from '../shared/types'

interface PersistedProjects {
  projects: Project[]
  tombstones: SyncTombstone[]
}

export class ProjectStore {
  private readonly file: string
  private projects: Project[] = []
  private _tombstones: SyncTombstone[] = []

  constructor() {
    this.file = join(app.getPath('userData'), 'projects.json')
    this.load()
  }

  private load(): void {
    try {
      if (existsSync(this.file)) {
        const raw = JSON.parse(readFileSync(this.file, 'utf-8'))
        if (Array.isArray(raw)) {
          this.projects = raw
          this._tombstones = []
        } else {
          this.projects = raw.projects ?? []
          this._tombstones = raw.tombstones ?? []
        }
      }
    } catch {
      this.projects = []
      this._tombstones = []
    }
  }

  private save(): void {
    mkdirSync(dirname(this.file), { recursive: true })
    const payload: PersistedProjects = { projects: this.projects, tombstones: this._tombstones }
    writeFileSync(this.file, JSON.stringify(payload, null, 2), 'utf-8')
  }

  list(): Project[] {
    return [...this.projects].sort(
      (a, b) => a.priority - b.priority || a.name.localeCompare(b.name, 'zh-Hans-CN')
    )
  }

  all(): Project[] {
    return [...this.projects]
  }

  tombstones(): SyncTombstone[] {
    return [...this._tombstones]
  }

  get(id: string): Project | undefined {
    return this.projects.find((p) => p.id === id)
  }

  create(input: ProjectInput): Project {
    const now = Date.now()
    const project: Project = {
      id: randomUUID(),
      name: input.name.trim(),
      priority: input.priority,
      createdAt: now,
      updatedAt: now
    }
    this.projects.push(project)
    this.save()
    return project
  }

  update(id: string, patch: ProjectUpdate): Project | null {
    const project = this.projects.find((p) => p.id === id)
    if (!project) return null
    if (patch.name !== undefined) project.name = patch.name.trim()
    if (patch.priority !== undefined) project.priority = patch.priority
    project.updatedAt = Date.now()
    this.save()
    return project
  }

  remove(id: string): boolean {
    const index = this.projects.findIndex((p) => p.id === id)
    if (index === -1) return false
    this.projects.splice(index, 1)
    this._tombstones.push({ kind: 'project', id, updatedAt: Date.now() })
    this.save()
    return true
  }

  applyRemote(resp: SyncResponse): void {
    // 服务端 live 记录：本地没有或时间戳更新则覆盖
    const byId = new Map(this.projects.map((p) => [p.id, p]))
    for (const p of resp.projects) {
      const cur = byId.get(p.id)
      if (!cur || p.updatedAt >= cur.updatedAt) {
        byId.set(p.id, p)
      }
    }
    this.projects = [...byId.values()]

    // 服务端墓碑：本地有且较旧则删除
    for (const tb of resp.tombstones) {
      if (tb.kind !== 'project') continue
      const cur = byId.get(tb.id)
      if (cur && tb.updatedAt >= cur.updatedAt) {
        this.projects = this.projects.filter((p) => p.id !== tb.id)
      }
    }

    // 墓碑以服务端为准（服务端是已见过所有客户端的超集）
    this._tombstones = resp.tombstones.filter((tb) => tb.kind === 'project')
    this.save()
  }
}
