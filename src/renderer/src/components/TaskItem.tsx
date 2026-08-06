import { useState } from 'react'
import type { Task, TaskInput, TaskPriority, TaskStatus, TaskUpdate } from '../../../shared/types'
import { TASK_STATUSES, STATUS_LABEL } from '../../../shared/types'

interface Props {
  task: Task
  subtasks: Task[]
  hideCompleted: boolean
  now: number
  onToggle: (task: Task) => void
  onUpdate: (id: string, patch: TaskUpdate) => void
  onRemove: (id: string) => void
}

const PRIORITY_LABEL: Record<TaskPriority, string> = {
  high: '高',
  medium: '中',
  low: '低'
}

function TaskItem({
  task,
  subtasks,
  hideCompleted,
  now,
  onToggle,
  onUpdate,
  onRemove
}: Props): React.JSX.Element {
  const [editing, setEditing] = useState(false)
  const [subtasksOpen, setSubtasksOpen] = useState(true)
  const [draft, setDraft] = useState<TaskInput>({
    parentId: task.parentId,
    title: task.title,
    description: task.description,
    status: task.status,
    priority: task.priority,
    dueAt: task.dueAt
  })

  const isSubtask = task.parentId !== null
  const completed = task.status === 'completed'
  const visibleSubtasks = hideCompleted
    ? subtasks.filter((s) => s.status !== 'completed')
    : subtasks
  const doneCount = subtasks.filter((s) => s.status === 'completed').length

  const startEdit = (): void => {
    setDraft({
      parentId: task.parentId,
      title: task.title,
      description: task.description,
      status: task.status,
      priority: task.priority,
      dueAt: task.dueAt
    })
    setEditing(true)
  }

  const saveEdit = (): void => {
    if (!draft.title.trim()) return
    onUpdate(task.id, { ...draft, title: draft.title.trim() })
    setEditing(false)
  }

  if (editing) {
    return (
      <li className={`task-item editing priority-${task.priority}${isSubtask ? ' subtask' : ''}`}>
        <input
          className="input"
          value={draft.title}
          onChange={(e) => setDraft((d) => ({ ...d, title: e.target.value }))}
        />
        <textarea
          className="input textarea"
          value={draft.description}
          onChange={(e) => setDraft((d) => ({ ...d, description: e.target.value }))}
        />
        <div className="task-actions">
          <select
            className="input select"
            value={draft.status}
            onChange={(e) => setDraft((d) => ({ ...d, status: e.target.value as TaskStatus }))}
          >
            {TASK_STATUSES.map((s) => (
              <option key={s} value={s}>
                {STATUS_LABEL[s]}
              </option>
            ))}
          </select>
          <select
            className="input select"
            value={draft.priority}
            onChange={(e) => setDraft((d) => ({ ...d, priority: e.target.value as TaskPriority }))}
          >
            <option value="high">高优先级</option>
            <option value="medium">中优先级</option>
            <option value="low">低优先级</option>
          </select>
          <input
            className="input"
            type="date"
            value={draft.dueAt ? new Date(draft.dueAt).toISOString().slice(0, 10) : ''}
            onChange={(e) =>
              setDraft((d) => ({
                ...d,
                dueAt: e.target.value ? new Date(e.target.value + 'T23:59:59.999').getTime() : null
              }))
            }
          />
          <button className="btn primary" onClick={saveEdit} disabled={!draft.title.trim()}>
            保存
          </button>
          <button className="btn" onClick={() => setEditing(false)}>
            取消
          </button>
        </div>
      </li>
    )
  }

  return (
    <div className="task-group">
      <li
        className={`task-item priority-${task.priority}${completed ? ' completed' : ''}${
          isSubtask ? ' subtask' : ''
        }`}
      >
        {!isSubtask && (
          <button
            className="subtask-toggle"
            title={subtasksOpen ? '隐藏子任务' : '显示子任务'}
            onClick={() => setSubtasksOpen((v) => !v)}
          >
            <span className="subtask-arrow">{subtasksOpen ? '▾' : '▸'}</span>
            <span className="subtask-progress">
              {subtasks.length > 0 ? `${doneCount}/${subtasks.length}` : '0'}
            </span>
          </button>
        )}
        <input
          className="checkbox"
          type="checkbox"
          checked={completed}
          onChange={() => onToggle(task)}
        />
        <div className="task-body">
          <span className="task-title">{task.title}</span>
          <select
            className={`status-select status-${task.status}`}
            value={task.status}
            onChange={(e) => onUpdate(task.id, { status: e.target.value as TaskStatus })}
          >
            {TASK_STATUSES.map((s) => (
              <option key={s} value={s}>
                {STATUS_LABEL[s]}
              </option>
            ))}
          </select>
          <span className={`badge priority-${task.priority}`}>
            {PRIORITY_LABEL[task.priority]}优先级
          </span>
          {task.dueAt && (
            <span className={`due${task.dueAt < now ? ' overdue' : ''}`}>
              {formatCountdown(task.dueAt, now)}
            </span>
          )}
          {task.description && <span className="task-desc">{task.description}</span>}
        </div>
        <div className="task-actions">
          <button className="btn" onClick={startEdit}>
            编辑
          </button>
          <button className="btn danger" onClick={() => onRemove(task.id)}>
            删除
          </button>
        </div>
      </li>

      {!isSubtask && (
        <div className="subtask-panel">
          {subtasksOpen && (
            <div className="subtask-area">
              {visibleSubtasks.length === 0 ? (
                <p className="subtask-empty">暂无子任务</p>
              ) : (
                <div className="subtask-list">
                  {visibleSubtasks.map((st) => (
                    <TaskItem
                      key={st.id}
                      task={st}
                      subtasks={[]}
                      hideCompleted={hideCompleted}
                      now={now}
                      onToggle={onToggle}
                      onUpdate={onUpdate}
                      onRemove={onRemove}
                    />
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function formatCountdown(dueAt: number, now: number): string {
  const diff = dueAt - now
  const abs = Math.abs(diff)
  const day = 86_400_000
  const hour = 3_600_000
  const minute = 60_000

  if (diff < 0) {
    if (abs >= day) return `已过期 ${Math.floor(abs / day)} 天`
    if (abs >= hour) return `已过期 ${Math.floor(abs / hour)} 小时`
    return `已过期 ${Math.max(1, Math.floor(abs / minute))} 分钟`
  }
  if (abs >= day) return `剩 ${Math.floor(abs / day)} 天`
  if (abs >= hour) return `剩 ${Math.floor(abs / hour)} 小时`
  if (abs >= minute) return `剩 ${Math.floor(abs / minute)} 分钟`
  return '即将截止'
}

export default TaskItem
