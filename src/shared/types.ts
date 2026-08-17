export type TaskPriority = 'low' | 'medium' | 'high'

export type TaskStatus = 'not_started' | 'in_progress' | 'testing' | 'completed'

export const TASK_STATUSES: TaskStatus[] = ['not_started', 'in_progress', 'testing', 'completed']

export const STATUS_LABEL: Record<TaskStatus, string> = {
  not_started: '未开始',
  in_progress: '进行中',
  testing: '测试中',
  completed: '已完成'
}

export interface Task {
  id: string
  parentId: string | null
  projectId: string | null
  title: string
  description: string
  status: TaskStatus
  priority: TaskPriority
  dueAt: number | null
  createdAt: number
  updatedAt: number
}

export interface TaskInput {
  parentId: string | null
  projectId: string | null
  title: string
  description: string
  status: TaskStatus
  priority: TaskPriority
  dueAt: number | null
}

export type TaskUpdate = Partial<TaskInput>

export interface Project {
  id: string
  name: string
  priority: number
  createdAt: number
  updatedAt: number
}

export interface ProjectInput {
  name: string
  priority: number
}

export type ProjectUpdate = Partial<ProjectInput>

export interface SyncConfig {
  serverUrl: string
  token: string
  certPath: string
  userName: string
}

export type SyncState = 'idle' | 'syncing' | 'ok' | 'error' | 'offline'

export interface SyncStatus {
  state: SyncState
  lastSyncAt: number | null
  message: string
}

export interface SyncTombstone {
  kind: 'task' | 'project'
  id: string
  updatedAt: number
}

export interface SyncRequest {
  tasks: Task[]
  projects: Project[]
  deleted: SyncTombstone[]
}

export interface SyncResponse {
  tasks: Task[]
  projects: Project[]
  tombstones: SyncTombstone[]
}
