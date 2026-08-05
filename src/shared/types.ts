export type TaskPriority = 'low' | 'medium' | 'high'

export interface Task {
  id: string
  title: string
  description: string
  completed: boolean
  priority: TaskPriority
  dueAt: number | null
  createdAt: number
  updatedAt: number
}

export interface TaskInput {
  title: string
  description: string
  priority: TaskPriority
  dueAt: number | null
}

export type TaskUpdate = Partial<TaskInput> & { completed?: boolean }
