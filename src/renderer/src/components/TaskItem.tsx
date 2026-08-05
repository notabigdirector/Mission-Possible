import { useState } from 'react'
import type { Task, TaskInput, TaskPriority } from '../../../shared/types'

interface Props {
  task: Task
  onToggle: (task: Task) => void
  onUpdate: (id: string, patch: Partial<TaskInput> & { completed?: boolean }) => void
  onRemove: (id: string) => void
}

const PRIORITY_LABEL: Record<TaskPriority, string> = {
  high: '高',
  medium: '中',
  low: '低'
}

function TaskItem({ task, onToggle, onUpdate, onRemove }: Props): React.JSX.Element {
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState({
    title: task.title,
    description: task.description,
    priority: task.priority,
    dueAt: task.dueAt
  })

  const startEdit = (): void => {
    setDraft({
      title: task.title,
      description: task.description,
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
      <li className={`task-item editing priority-${task.priority}`}>
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
                dueAt: e.target.value ? new Date(e.target.value + 'T00:00:00').getTime() : null
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
    <li className={`task-item priority-${task.priority}${task.completed ? ' completed' : ''}`}>
      <input
        className="checkbox"
        type="checkbox"
        checked={task.completed}
        onChange={() => onToggle(task)}
      />
      <div className="task-body">
        <div className="task-title">{task.title}</div>
        {task.description && <div className="task-desc">{task.description}</div>}
        <div className="task-meta">
          <span className={`badge priority-${task.priority}`}>
            {PRIORITY_LABEL[task.priority]}优先级
          </span>
          {task.dueAt && <span className="due">截止 {formatDate(task.dueAt)}</span>}
        </div>
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
  )
}

function formatDate(timestamp: number): string {
  const d = new Date(timestamp)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export default TaskItem
