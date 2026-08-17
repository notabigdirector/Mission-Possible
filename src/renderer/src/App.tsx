import { useCallback, useEffect, useMemo, useState } from 'react'
import type {
  Project,
  ProjectInput,
  ProjectUpdate,
  SyncConfig,
  SyncStatus,
  Task,
  TaskInput,
  TaskStatus,
  TaskUpdate
} from '../../shared/types'
import TaskForm from './components/TaskForm'
import TaskItem from './components/TaskItem'
import TaskModal from './components/TaskModal'
import ProjectManager from './components/ProjectManager'
import SyncSettings from './components/SyncSettings'

type Filter = 'all' | TaskStatus

type SortMode = 'default' | 'name' | 'countdown' | 'priority'

type ProjectFilter = 'all' | 'none' | string

function App(): React.JSX.Element {
  const [tasks, setTasks] = useState<Task[]>([])
  const [projects, setProjects] = useState<Project[]>([])
  const [filter, setFilter] = useState<Filter>('all')
  const [hideCompleted, setHideCompleted] = useState(true)
  const [sortMode, setSortMode] = useState<SortMode>('priority')
  const [projectFilter, setProjectFilter] = useState<ProjectFilter>('all')
  const [showProjectManager, setShowProjectManager] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [now, setNow] = useState(() => Date.now())
  const [version, setVersion] = useState('')
  const [detailTask, setDetailTask] = useState<Task | null>(null)
  const [showSyncSettings, setShowSyncSettings] = useState(false)
  const [syncConfig, setSyncConfig] = useState<SyncConfig | null>(null)
  const [syncStatus, setSyncStatus] = useState<SyncStatus | null>(null)

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 30_000)
    return () => clearInterval(id)
  }, [])

  useEffect(() => {
    window.api.app
      .version()
      .then(setVersion)
      .catch(() => {})
  }, [])

  useEffect(() => {
    window.api.sync
      .getConfig()
      .then(setSyncConfig)
      .catch(() => {})
    window.api.sync
      .status()
      .then(setSyncStatus)
      .catch(() => {})
    return window.api.sync.onStatusChanged(setSyncStatus)
  }, [])

  const refresh = useCallback(async (): Promise<void> => {
    const list = await window.api.tasks.list()
    setTasks(list)
  }, [])

  const refreshProjects = useCallback(async (): Promise<void> => {
    const list = await window.api.projects.list()
    setProjects(list)
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

  useEffect(() => {
    let cancelled = false
    window.api.projects
      .list()
      .then((list) => {
        if (!cancelled) setProjects(list)
      })
      .catch(() => {
        if (!cancelled) setError('无法加载项目数据')
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

  const handleCreateProject = useCallback(
    async (input: ProjectInput): Promise<void> => {
      try {
        await window.api.projects.create(input)
        await refreshProjects()
      } catch {
        setError('创建项目失败')
      }
    },
    [refreshProjects]
  )

  const handleUpdateProject = useCallback(
    async (id: string, patch: ProjectUpdate): Promise<void> => {
      try {
        await window.api.projects.update(id, patch)
        await refreshProjects()
      } catch {
        setError('更新项目失败')
      }
    },
    [refreshProjects]
  )

  const handleSaveSyncConfig = useCallback(async (config: SyncConfig): Promise<void> => {
    const saved = await window.api.sync.setConfig(config)
    setSyncConfig(saved)
  }, [])

  const handleRemoveProject = useCallback(
    async (id: string): Promise<void> => {
      try {
        await window.api.projects.remove(id)
        await Promise.all([refreshProjects(), refresh()])
        setProjectFilter((cur) => (cur === id ? 'all' : cur))
      } catch {
        setError('删除项目失败')
      }
    },
    [refreshProjects, refresh]
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

  const sortedProjects = useMemo(
    () =>
      [...projects].sort(
        (a, b) => a.priority - b.priority || a.name.localeCompare(b.name, 'zh-Hans-CN')
      ),
    [projects]
  )

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
      } else if (sortMode === 'priority') {
        const rank: Record<string, number> = { high: 0, medium: 1, low: 2 }
        arr.sort((a, b) => {
          const byPriority = rank[a.priority] - rank[b.priority]
          if (byPriority !== 0) return byPriority
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

  const baseFiltered = useMemo(() => {
    let top = tasks.filter((t) => !t.parentId)
    if (filter !== 'all') top = top.filter((t) => t.status === filter)
    if (hideCompleted) top = top.filter((t) => t.status !== 'completed')
    return top
  }, [tasks, filter, hideCompleted])

  const visibleTasks = useMemo(() => {
    let list = baseFiltered
    if (projectFilter === 'none') list = list.filter((t) => !t.projectId)
    else if (projectFilter !== 'all') list = list.filter((t) => t.projectId === projectFilter)
    return sortTasks(list)
  }, [baseFiltered, projectFilter, sortTasks])

  const projectGroups = useMemo(() => {
    const map = new Map<string, { project: Project | null; tasks: Task[] }>()
    for (const t of baseFiltered) {
      const project = t.projectId ? projects.find((p) => p.id === t.projectId) : undefined
      const key = project ? project.id : ''
      const group = map.get(key) ?? { project: project ?? null, tasks: [] }
      group.tasks.push(t)
      map.set(key, group)
    }
    const arr = [...map.values()]
    arr.sort((a, b) => {
      if (a.project && b.project) {
        const byPriority = a.project.priority - b.project.priority
        if (byPriority !== 0) return byPriority
        return a.project.name.localeCompare(b.project.name, 'zh-Hans-CN')
      }
      if (a.project) return -1
      if (b.project) return 1
      return 0
    })
    return arr
  }, [baseFiltered, projects])

  const countBy = useCallback(
    (status: TaskStatus): number => tasks.filter((t) => t.status === status).length,
    [tasks]
  )

  return (
    <div className="app">
      <div className="app-top">
        <header className="app-header">
          <div className="app-title">
            <h1>任务管理</h1>
            {version && <span className="app-version">v{version}</span>}
            {syncStatus && (
              <span className={`sync-indicator ${syncStatus.state}`}>
                {syncStatus.state === 'ok' && '已同步'}
                {syncStatus.state === 'syncing' && '同步中…'}
                {syncStatus.state === 'error' && '同步出错'}
                {syncStatus.state === 'offline' && '离线'}
                {syncStatus.state === 'idle' && '未同步'}
              </span>
            )}
          </div>
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
          <label className="project-filter">
            项目
            <select
              className="input select"
              value={projectFilter}
              onChange={(e) => setProjectFilter(e.target.value as ProjectFilter)}
            >
              <option value="all">全部项目</option>
              <option value="none">无项目</option>
              {sortedProjects.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}（{p.priority}）
                </option>
              ))}
            </select>
          </label>
          <button className="btn" onClick={() => setShowProjectManager(true)}>
            管理项目
          </button>
          <button className="btn" onClick={() => setShowSyncSettings(true)}>
            ☁ 同步设置
          </button>
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
              <option value="priority">按优先级</option>
            </select>
          </label>
        </header>

        {error && (
          <div className="error-banner" onClick={() => setError(null)}>
            {error}（点击关闭）
          </div>
        )}

        <TaskForm parents={mainTasks} projects={sortedProjects} onCreate={handleCreate} />
      </div>

      <main className="task-list">
        {projectFilter === 'all' ? (
          projectGroups.length === 0 ? (
            <p className="empty-tip">暂无任务，从上方添加一个吧</p>
          ) : (
            projectGroups.map((g) => (
              <div className="project-group" key={g.project ? g.project.id : 'none'}>
                <div className="project-header">
                  <span className="project-name">{g.project ? g.project.name : '无项目'}</span>
                  {g.project && <span className="project-priority">优先级 {g.project.priority}</span>}
                  <span className="project-count">{g.tasks.length} 项</span>
                </div>
                {sortTasks(g.tasks).map((task) => (
                  <TaskItem
                    key={task.id}
                    task={task}
                    subtasks={sortTasks(groups.get(task.id) ?? [])}
                    hideCompleted={hideCompleted}
                    now={now}
                    onToggle={handleToggle}
                    onUpdate={handleUpdate}
                    onRemove={handleRemove}
                    onOpenDetail={setDetailTask}
                  />
                ))}
              </div>
            ))
          )
        ) : visibleTasks.length === 0 ? (
          <p className="empty-tip">该分组下没有任务</p>
        ) : (
          visibleTasks.map((task) => (
            <TaskItem
              key={task.id}
              task={task}
              subtasks={sortTasks(groups.get(task.id) ?? [])}
              hideCompleted={hideCompleted}
              now={now}
              onToggle={handleToggle}
              onUpdate={handleUpdate}
              onRemove={handleRemove}
              onOpenDetail={setDetailTask}
            />
          ))
        )}
      </main>

      {detailTask && (
        <TaskModal
          task={detailTask}
          parents={mainTasks.filter((t) => t.id !== detailTask.id)}
          projects={sortedProjects}
          onSave={handleUpdate}
          onClose={() => setDetailTask(null)}
        />
      )}

      {showProjectManager && (
        <ProjectManager
          projects={sortedProjects}
          onCreate={handleCreateProject}
          onUpdate={handleUpdateProject}
          onRemove={handleRemoveProject}
          onClose={() => setShowProjectManager(false)}
        />
      )}

      {showSyncSettings && syncConfig && (
        <SyncSettings
          config={syncConfig}
          status={syncStatus ?? { state: 'idle', lastSyncAt: null, message: '' }}
          onSave={handleSaveSyncConfig}
          onClose={() => setShowSyncSettings(false)}
        />
      )}
    </div>
  )
}

export default App
