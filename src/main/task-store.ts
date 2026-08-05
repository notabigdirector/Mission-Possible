import { app } from 'electron'
import { dirname, join } from 'path'
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'fs'
import { randomUUID } from 'crypto'
import type { Task, TaskInput, TaskUpdate } from '../shared/types'

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
  }

  private save(): void {
    mkdirSync(dirname(this.file), { recursive: true })
    writeFileSync(this.file, JSON.stringify(this.tasks, null, 2), 'utf-8')
  }

  list(): Task[] {
    return [...this.tasks].sort((a, b) => {
      if (a.completed !== b.completed) return a.completed ? 1 : -1
      return b.createdAt - a.createdAt
    })
  }

  get(id: string): Task | undefined {
    return this.tasks.find((t) => t.id === id)
  }

  create(input: TaskInput): Task {
    const now = Date.now()
    const task: Task = {
      id: randomUUID(),
      title: input.title.trim(),
      description: input.description.trim(),
      completed: false,
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
    if (patch.priority !== undefined) task.priority = patch.priority
    if (patch.dueAt !== undefined) task.dueAt = patch.dueAt
    if (patch.completed !== undefined) task.completed = patch.completed
    task.updatedAt = Date.now()

    this.save()
    return task
  }

  remove(id: string): boolean {
    const index = this.tasks.findIndex((t) => t.id === id)
    if (index === -1) return false
    this.tasks.splice(index, 1)
    this.save()
    return true
  }
}
