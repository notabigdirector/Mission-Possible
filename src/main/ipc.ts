import { ipcMain } from 'electron'
import type { ProjectInput, ProjectUpdate, TaskInput, TaskUpdate } from '../shared/types'
import { ProjectStore } from './project-store'
import { TaskStore } from './task-store'

export const taskIpc = {
  register(store: TaskStore): void {
    ipcMain.handle('tasks:list', () => store.list())
    ipcMain.handle('tasks:get', (_e, id: string) => store.get(id) ?? null)
    ipcMain.handle('tasks:create', (_e, input: TaskInput) => store.create(input))
    ipcMain.handle('tasks:update', (_e, id: string, patch: TaskUpdate) => store.update(id, patch))
    ipcMain.handle('tasks:remove', (_e, id: string) => store.remove(id))
  }
}

export const projectIpc = {
  register(store: ProjectStore, tasks: TaskStore): void {
    ipcMain.handle('projects:list', () => store.list())
    ipcMain.handle('projects:create', (_e, input: ProjectInput) => store.create(input))
    ipcMain.handle('projects:update', (_e, id: string, patch: ProjectUpdate) =>
      store.update(id, patch)
    )
    ipcMain.handle('projects:remove', (_e, id: string) => {
      const ok = store.remove(id)
      if (ok) tasks.clearProject(id)
      return ok
    })
  }
}
