import { useCallback, useEffect, useMemo, useState } from 'react'
import type { Task, TaskInput, TaskStatus, TaskUpdate } from '../../shared/types'
import TaskForm from './components/TaskForm'
import TaskItem from './components/TaskItem'

type Filter = 'all' | TaskStatus

type SortMode = 'default' | 'name' | 'countdown'

function App(): React.JSX.Element {
  const [tasks, setTasks] = useState<Task[]>([])
  const [filter, setFilter] = useState<Filter>('all')
  const [hideCompleted, setHideCompleted] = useState(true)
  const [sortMode, setSortMode] = useState<SortMode>('default')
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

  const groups = useMemo(() => {
    const map = new Map<string, Task[]>()
    for (const t of tasks) {
      if (!t.parentId) continue
      const arr = map.get(t.parentId) ?? []
      arr.push(t)
      map.set(t.parentId, arr)
    }
    return map
  }, [tasks])

  const mainTasks = useMemo(() => tasks.filter((t) => !t.parentId), [tasks])

  const sortTasks = useCallback(
    (list: Task[]): Task[] => {
      const arr = [...list]
      if (sortMode === 'name') {
        arr.sort((a, b) => a.title.localeCompare(b.title, 'zh-Hans-CN'))
      } else if (sortMode === 'countdown') {
        arr.sort((a, b) => {
          if (a.dueAt !== null && b.dueAt !== null) return a.dueAt - b.dueAt
          if (a.dueAt !== null) return -1
          if (b.dueAt !== null) return 1
          return a.createdAt - b.createdAt
        })
      } else {
        arr.sort((a, b) => a.createdAt - b.createdAt)
      }
      return arr
    },
    [sortMode]
  )

  const filtered = useMemo(() => {
    let top = tasks.filter((t) => !t.parentId)
    if (filter !== 'all') top = top.filter((t) => t.status === filter)
    if (hideCompleted) top = top.filter((t) => t.status !== 'completed')
    return sortTasks(top)
  }, [tasks, filter, hideCompleted, sortTasks])

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
        <label className="sort-mode">
          排序
          <select
            className="input select"
            value={sortMode}
            onChange={(e) => setSortMode(e.target.value as SortMode)}
          >
            <option value="default">默认</option>
            <option value="name">按名称</option>
            <option value="countdown">按倒计时</option>
          </select>
        </label>
      </header>

      {error && (
        <div className="error-banner" onClick={() => setError(null)}>
          {error}（点击关闭）
        </div>
      )}

      <TaskForm parents={mainTasks} onCreate={handleCreate} />

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
              subtasks={sortTasks(groups.get(task.id) ?? [])}
              hideCompleted={hideCompleted}
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
