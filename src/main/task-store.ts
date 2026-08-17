import { app } from 'electron'
import { dirname, join } from 'path'
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'fs'
import { randomUUID } from 'crypto'
import type { SyncResponse, SyncTombstone, Task, TaskInput, TaskUpdate, TaskStatus } from '../shared/types'

const STATUS_ORDER: Record<TaskStatus, number> = {
  not_started: 0,
  in_progress: 1,
  testing: 2,
  completed: 3
}

interface PersistedTasks {
  tasks: Task[]
  tombstones: SyncTombstone[]
}

export class TaskStore {
  private readonly file: string
  private tasks: Task[] = []
  private _tombstones: SyncTombstone[] = []

  constructor() {
    this.file = join(app.getPath('userData'), 'tasks.json')
    this.load()
  }

  private load(): void {
    try {
      if (existsSync(this.file)) {
        const raw = JSON.parse(readFileSync(this.file, 'utf-8'))
        if (Array.isArray(raw)) {
          this.tasks = raw
          this._tombstones = []
        } else {
          this.tasks = raw.tasks ?? []
          this._tombstones = raw.tombstones ?? []
        }
      }
    } catch {
      this.tasks = []
      this._tombstones = []
    }
    // 兼容旧数据：completed 布尔值迁移为 status 字段
    this.tasks = this.tasks.map((t) => {
      if (!t.status) {
        t.status = (t as unknown as { completed?: boolean }).completed ? 'completed' : 'not_started'
      }
      if (t.parentId === undefined) t.parentId = null
      if (t.projectId === undefined) t.projectId = null
      return t
    })
    this.cleanupOrphans()
  }

  private cleanupOrphans(): void {
    const ids = new Set(this.tasks.map((t) => t.id))
    const orphans = this.tasks.filter((t) => t.parentId && !ids.has(t.parentId))
    this.tasks = this.tasks.filter((t) => !t.parentId || ids.has(t.parentId))
    const now = Date.now()
    for (const o of orphans) {
      this._tombstones.push({ kind: 'task', id: o.id, updatedAt: now })
    }
  }

  private save(): void {
    mkdirSync(dirname(this.file), { recursive: true })
    const payload: PersistedTasks = { tasks: this.tasks, tombstones: this._tombstones }
    writeFileSync(this.file, JSON.stringify(payload, null, 2), 'utf-8')
  }

  list(): Task[] {
    return [...this.tasks].sort((a, b) => {
      if (a.status !== b.status) return STATUS_ORDER[a.status] - STATUS_ORDER[b.status]
      return b.createdAt - a.createdAt
    })
  }

  all(): Task[] {
    return [...this.tasks]
  }

  tombstones(): SyncTombstone[] {
    return [...this._tombstones]
  }

  get(id: string): Task | undefined {
    return this.tasks.find((t) => t.id === id)
  }

  create(input: TaskInput): Task {
    const now = Date.now()
    let projectId = input.projectId ?? null
    if (input.parentId) {
      const parent = this.tasks.find((t) => t.id === input.parentId)
      if (!parent || parent.parentId) throw new Error('无效的父任务')
      projectId = parent.projectId
    }
    const task: Task = {
      id: randomUUID(),
      parentId: input.parentId,
      projectId,
      title: input.title.trim(),
      description: input.description.trim(),
      status: input.status,
      priority: input.priority,
      dueAt: input.dueAt,
      createdAt: now,
      updatedAt: now
    }
    this.tasks.push(task)
    this.save()
    return task
  }

  update(id: string, patch: TaskUpdate): Task | null {
    const task = this.tasks.find((t) => t.id === id)
    if (!task) return null

    if (patch.title !== undefined) task.title = patch.title.trim()
    if (patch.description !== undefined) task.description = patch.description.trim()
    if (patch.status !== undefined) task.status = patch.status
    if (patch.priority !== undefined) task.priority = patch.priority
    if (patch.dueAt !== undefined) task.dueAt = patch.dueAt
    if (patch.projectId !== undefined) {
      task.projectId = patch.projectId
      if (!task.parentId) {
        for (const t of this.tasks) {
          if (t.parentId === task.id) t.projectId = patch.projectId
        }
      }
    }
    task.updatedAt = Date.now()

    this.save()
    return task
  }

  clearProject(projectId: string): void {
    for (const t of this.tasks) {
      if (t.projectId === projectId) {
        t.projectId = null
        t.updatedAt = Date.now()
      }
    }
    this.save()
  }

  remove(id: string): boolean {
    const index = this.tasks.findIndex((t) => t.id === id)
    if (index === -1) return false
    const now = Date.now()
    const removed = this.tasks.filter((t) => t.id === id || t.parentId === id)
    for (const t of removed) {
      this._tombstones.push({ kind: 'task', id: t.id, updatedAt: now })
    }
    this.tasks = this.tasks.filter((t) => !removed.some((r) => r.id === t.id))
    this.save()
    return true
  }

  applyRemote(resp: SyncResponse): void {
    // 服务端 live 记录：本地没有或时间戳更新则覆盖
    const byId = new Map(this.tasks.map((t) => [t.id, t]))
    for (const t of resp.tasks) {
      const cur = byId.get(t.id)
      if (!cur || t.updatedAt >= cur.updatedAt) {
        byId.set(t.id, t)
      }
    }
    this.tasks = [...byId.values()]

    // 服务端墓碑：本地有且较旧则删除
    for (const tb of resp.tombstones) {
      if (tb.kind !== 'task') continue
      const cur = byId.get(tb.id)
      if (cur && tb.updatedAt >= cur.updatedAt) {
        this.tasks = this.tasks.filter((t) => t.id !== tb.id)
      }
    }

    // 墓碑以服务端为准（服务端是已见过所有客户端的超集）
    this._tombstones = resp.tombstones.filter((tb) => tb.kind === 'task')
    this.cleanupOrphans()
    this.save()
  }
}
