import { contextBridge, ipcRenderer } from 'electron'
import { electronAPI } from '@electron-toolkit/preload'
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

export interface RegisterResult {
  token: string
  user: { id: string; name: string }
}

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
  },
  sync: {
    getConfig: (): Promise<SyncConfig> => ipcRenderer.invoke('sync:get-config'),
    setConfig: (config: SyncConfig): Promise<SyncConfig> =>
      ipcRenderer.invoke('sync:set-config', config),
    register: (name: string): Promise<RegisterResult> => ipcRenderer.invoke('sync:register', name),
    now: (): Promise<SyncStatus> => ipcRenderer.invoke('sync:now'),
    status: (): Promise<SyncStatus> => ipcRenderer.invoke('sync:status'),
    onStatusChanged: (cb: (status: SyncStatus) => void): (() => void) => {
      const listener = (_e: Electron.IpcRendererEvent, status: SyncStatus): void => cb(status)
      ipcRenderer.on('sync:status-changed', listener)
      return () => ipcRenderer.removeListener('sync:status-changed', listener)
    }
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
