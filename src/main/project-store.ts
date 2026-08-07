import { app } from 'electron'
import { dirname, join } from 'path'
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'fs'
import { randomUUID } from 'crypto'
import type { Project, ProjectInput, ProjectUpdate } from '../shared/types'

export class ProjectStore {
  private readonly file: string
  private projects: Project[] = []

  constructor() {
    this.file = join(app.getPath('userData'), 'projects.json')
    this.load()
  }

  private load(): void {
    try {
      if (existsSync(this.file)) {
        this.projects = JSON.parse(readFileSync(this.file, 'utf-8'))
      }
    } catch {
      this.projects = []
    }
  }

  private save(): void {
    mkdirSync(dirname(this.file), { recursive: true })
    writeFileSync(this.file, JSON.stringify(this.projects, null, 2), 'utf-8')
  }

  list(): Project[] {
    return [...this.projects].sort(
      (a, b) => a.priority - b.priority || a.name.localeCompare(b.name, 'zh-Hans-CN')
    )
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
    this.save()
    return true
  }
}
