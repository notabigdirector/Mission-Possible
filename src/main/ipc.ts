import { ipcMain } from 'electron'
import type { TaskInput, TaskUpdate } from '../shared/types'
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
