import { useCallback, useEffect, useMemo, useState } from 'react'
import type { Task, TaskInput } from '../../shared/types'
import TaskForm from './components/TaskForm'
import TaskItem from './components/TaskItem'

type Filter = 'all' | 'active' | 'completed'

function App(): React.JSX.Element {
  const [tasks, setTasks] = useState<Task[]>([])
  const [filter, setFilter] = useState<Filter>('all')
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async (): Promise<void> => {
    const list = await window.api.tasks.list()
    setTasks(list)
  }, [])

  useEffect(() => {
    let cancelled = false
    window.api.tasks
      .list()
      .then((list) => {
        if (!cancelled) setTasks(list)
      })
      .catch(() => {
        if (!cancelled) setError('无法加载任务数据')
      })
    return () => {
      cancelled = true
    }
  }, [])

  const handleCreate = useCallback(
    async (input: TaskInput): Promise<void> => {
      try {
        await window.api.tasks.create(input)
        await refresh()
      } catch {
        setError('创建任务失败')
      }
    },
    [refresh]
  )

  const handleToggle = useCallback(
    async (task: Task): Promise<void> => {
      try {
        await window.api.tasks.update(task.id, { completed: !task.completed })
        await refresh()
      } catch {
        setError('更新任务失败')
      }
    },
    [refresh]
  )

  const handleUpdate = useCallback(
    async (id: string, patch: Partial<TaskInput> & { completed?: boolean }): Promise<void> => {
      try {
        await window.api.tasks.update(id, patch)
        await refresh()
      } catch {
        setError('更新任务失败')
      }
    },
    [refresh]
  )

  const handleRemove = useCallback(
    async (id: string): Promise<void> => {
      try {
        await window.api.tasks.remove(id)
        await refresh()
      } catch {
        setError('删除任务失败')
      }
    },
    [refresh]
  )

  const filtered = useMemo(() => {
    switch (filter) {
      case 'active':
        return tasks.filter((t) => !t.completed)
      case 'completed':
        return tasks.filter((t) => t.completed)
      default:
        return tasks
    }
  }, [tasks, filter])

  const activeCount = useMemo(() => tasks.filter((t) => !t.completed).length, [tasks])

  return (
    <div className="app">
      <header className="app-header">
        <h1>任务管理</h1>
        <nav className="filters">
          <button
            className={filter === 'all' ? 'filter active' : 'filter'}
            onClick={() => setFilter('all')}
          >
            全部 ({tasks.length})
          </button>
          <button
            className={filter === 'active' ? 'filter active' : 'filter'}
            onClick={() => setFilter('active')}
          >
            进行中 ({activeCount})
          </button>
          <button
            className={filter === 'completed' ? 'filter active' : 'filter'}
            onClick={() => setFilter('completed')}
          >
            已完成 ({tasks.length - activeCount})
          </button>
        </nav>
      </header>

      {error && (
        <div className="error-banner" onClick={() => setError(null)}>
          {error}（点击关闭）
        </div>
      )}

      <TaskForm onCreate={handleCreate} />

      <main className="task-list">
        {filtered.length === 0 ? (
          <p className="empty-tip">
            {filter === 'all' ? '暂无任务，从上方添加一个吧' : '该分组下没有任务'}
          </p>
        ) : (
          filtered.map((task) => (
            <TaskItem
              key={task.id}
              task={task}
              onToggle={handleToggle}
              onUpdate={handleUpdate}
              onRemove={handleRemove}
            />
          ))
        )}
      </main>
    </div>
  )
}

export default App
