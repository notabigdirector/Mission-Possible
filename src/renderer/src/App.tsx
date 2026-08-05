import { useCallback, useEffect, useMemo, useState } from 'react'
import type { Task, TaskInput, TaskStatus, TaskUpdate } from '../../shared/types'
import TaskForm from './components/TaskForm'
import TaskItem from './components/TaskItem'

type Filter = 'all' | TaskStatus

function App(): React.JSX.Element {
  const [tasks, setTasks] = useState<Task[]>([])
  const [filter, setFilter] = useState<Filter>('all')
  const [hideCompleted, setHideCompleted] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 30_000)
    return () => clearInterval(id)
  }, [])

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
        const status: TaskStatus = task.status === 'completed' ? 'in_progress' : 'completed'
        await window.api.tasks.update(task.id, { status })
        await refresh()
      } catch {
        setError('更新任务失败')
      }
    },
    [refresh]
  )

  const handleUpdate = useCallback(
    async (id: string, patch: TaskUpdate): Promise<void> => {
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
    if (filter !== 'all') return tasks.filter((t) => t.status === filter)
    return hideCompleted ? tasks.filter((t) => t.status !== 'completed') : tasks
  }, [tasks, filter, hideCompleted])

  const countBy = useCallback(
    (status: TaskStatus): number => tasks.filter((t) => t.status === status).length,
    [tasks]
  )

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
            className={filter === 'not_started' ? 'filter active' : 'filter'}
            onClick={() => setFilter('not_started')}
          >
            未开始 ({countBy('not_started')})
          </button>
          <button
            className={filter === 'in_progress' ? 'filter active' : 'filter'}
            onClick={() => setFilter('in_progress')}
          >
            进行中 ({countBy('in_progress')})
          </button>
          <button
            className={filter === 'testing' ? 'filter active' : 'filter'}
            onClick={() => setFilter('testing')}
          >
            测试中 ({countBy('testing')})
          </button>
          <button
            className={filter === 'completed' ? 'filter active' : 'filter'}
            onClick={() => setFilter('completed')}
          >
            已完成 ({countBy('completed')})
          </button>
        </nav>
        <label className="hide-completed">
          <input
            type="checkbox"
            checked={hideCompleted}
            onChange={(e) => setHideCompleted(e.target.checked)}
          />
          隐藏已完成
        </label>
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
              now={now}
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
