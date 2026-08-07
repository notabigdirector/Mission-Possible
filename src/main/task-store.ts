import { app } from 'electron'
import { dirname, join } from 'path'
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'fs'
import { randomUUID } from 'crypto'
import type { Task, TaskInput, TaskUpdate, TaskStatus } from '../shared/types'

const STATUS_ORDER: Record<TaskStatus, number> = {
  not_started: 0,
  in_progress: 1,
  testing: 2,
  completed: 3
}

export class TaskStore {
  private readonly file: string
  private tasks: Task[] = []

  constructor() {
    this.file = join(app.getPath('userData'), 'tasks.json')
    this.load()
  }

  private load(): void {
    try {
      if (existsSync(this.file)) {
        this.tasks = JSON.parse(readFileSync(this.file, 'utf-8'))
      }
    } catch {
      this.tasks = []
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
    // 清理孤儿子任务（父任务已不存在）
    const ids = new Set(this.tasks.map((t) => t.id))
    this.tasks = this.tasks.filter((t) => !t.parentId || ids.has(t.parentId))
  }

  private save(): void {
    mkdirSync(dirname(this.file), { recursive: true })
    writeFileSync(this.file, JSON.stringify(this.tasks, null, 2), 'utf-8')
  }

  list(): Task[] {
    return [...this.tasks].sort((a, b) => {
      if (a.status !== b.status) return STATUS_ORDER[a.status] - STATUS_ORDER[b.status]
      return b.createdAt - a.createdAt
    })
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
      if (t.projectId === projectId) t.projectId = null
    }
    this.save()
  }

  remove(id: string): boolean {
    const index = this.tasks.findIndex((t) => t.id === id)
    if (index === -1) return false
    this.tasks.splice(index, 1)
    // 级联删除该任务下的所有子任务
    this.tasks = this.tasks.filter((t) => t.parentId !== id)
    this.save()
    return true
  }
}
