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
  title: string
  description: string
  status: TaskStatus
  priority: TaskPriority
  dueAt: number | null
}

export type TaskUpdate = Partial<TaskInput>
