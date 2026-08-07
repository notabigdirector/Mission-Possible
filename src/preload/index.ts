import { contextBridge, ipcRenderer } from 'electron'
import { electronAPI } from '@electron-toolkit/preload'
import type { Project, ProjectInput, ProjectUpdate, Task, TaskInput, TaskUpdate } from '../shared/types'

const api = {
  app: {
    version: (): Promise<string> => ipcRenderer.invoke('app:version')
  },
  tasks: {
    list: (): Promise<Task[]> => ipcRenderer.invoke('tasks:list'),
    get: (id: string): Promise<Task | null> => ipcRenderer.invoke('tasks:get', id),
    create: (input: TaskInput): Promise<Task> => ipcRenderer.invoke('tasks:create', input),
    update: (id: string, patch: TaskUpdate): Promise<Task | null> =>
      ipcRenderer.invoke('tasks:update', id, patch),
    remove: (id: string): Promise<boolean> => ipcRenderer.invoke('tasks:remove', id)
  },
  projects: {
    list: (): Promise<Project[]> => ipcRenderer.invoke('projects:list'),
    create: (input: ProjectInput): Promise<Project> => ipcRenderer.invoke('projects:create', input),
    update: (id: string, patch: ProjectUpdate): Promise<Project | null> =>
      ipcRenderer.invoke('projects:update', id, patch),
    remove: (id: string): Promise<boolean> => ipcRenderer.invoke('projects:remove', id)
  }
}

export type Api = typeof api

// Use `contextBridge` APIs to expose Electron APIs to
// renderer only if context isolation is enabled, otherwise
// just add to the DOM global.
if (process.contextIsolated) {
  try {
    contextBridge.exposeInMainWorld('electron', electronAPI)
    contextBridge.exposeInMainWorld('api', api)
  } catch (error) {
    console.error(error)
  }
} else {
  // @ts-ignore (define in dts)
  window.electron = electronAPI
  // @ts-ignore (define in dts)
  window.api = api
}
