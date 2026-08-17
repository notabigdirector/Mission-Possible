import { BrowserWindow, ipcMain } from 'electron'
import type {
  Project,
  ProjectInput,
  ProjectUpdate,
  SyncConfig,
  SyncStatus,
  Task,
  TaskInput,
  TaskUpdate
} from '../shared/types'
import { ProjectStore } from './project-store'
import { TaskStore } from './task-store'
import { SyncService } from './sync'

export const taskIpc = {
  register(store: TaskStore, sync: SyncService): void {
    ipcMain.handle('tasks:list', () => store.list())
    ipcMain.handle('tasks:get', (_e, id: string) => store.get(id) ?? null)
    ipcMain.handle('tasks:create', (_e, input: TaskInput): Task => {
      const task = store.create(input)
      sync.markDirty()
      return task
    })
    ipcMain.handle('tasks:update', (_e, id: string, patch: TaskUpdate): Task | null => {
      const task = store.update(id, patch)
      sync.markDirty()
      return task
    })
    ipcMain.handle('tasks:remove', (_e, id: string): boolean => {
      const ok = store.remove(id)
      sync.markDirty()
      return ok
    })
  }
}

export const projectIpc = {
  register(store: ProjectStore, tasks: TaskStore, sync: SyncService): void {
    ipcMain.handle('projects:list', () => store.list())
    ipcMain.handle('projects:create', (_e, input: ProjectInput): Project => {
      const project = store.create(input)
      sync.markDirty()
      return project
    })
    ipcMain.handle('projects:update', (_e, id: string, patch: ProjectUpdate): Project | null => {
      const project = store.update(id, patch)
      sync.markDirty()
      return project
    })
    ipcMain.handle('projects:remove', (_e, id: string): boolean => {
      const ok = store.remove(id)
      if (ok) tasks.clearProject(id)
      sync.markDirty()
      return ok
    })
  }
}

export const syncIpc = {
  register(sync: SyncService): void {
    ipcMain.handle('sync:get-config', () => sync.getConfig())
    ipcMain.handle('sync:set-config', (_e, config: SyncConfig) => sync.setConfig(config))
    ipcMain.handle('sync:register', (_e, name: string) => sync.registerUser(name))
    ipcMain.handle('sync:now', (): Promise<SyncStatus> => sync.sync())
    ipcMain.handle('sync:status', () => sync.getStatus())
    sync.onStatus((status): void => {
      for (const win of BrowserWindow.getAllWindows()) {
        win.webContents.send('sync:status-changed', status)
      }
    })
  }
}
