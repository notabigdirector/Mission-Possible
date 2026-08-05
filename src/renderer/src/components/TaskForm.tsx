import { useState } from 'react'
import type { TaskInput, TaskPriority, TaskStatus } from '../../../shared/types'
import { TASK_STATUSES, STATUS_LABEL } from '../../../shared/types'

const EMPTY: TaskInput = {
  title: '',
  description: '',
  status: 'not_started',
  priority: 'medium',
  dueAt: null
}

interface Props {
  onCreate: (input: TaskInput) => void
}

function TaskForm({ onCreate }: Props): React.JSX.Element {
  const [form, setForm] = useState<TaskInput>(EMPTY)

  const set = <K extends keyof TaskInput>(key: K, value: TaskInput[K]): void => {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  const handleSubmit = (e: React.FormEvent): void => {
    e.preventDefault()
    if (!form.title.trim()) return
    onCreate({ ...form, title: form.title.trim() })
    setForm(EMPTY)
  }

  return (
    <form className="task-form" onSubmit={handleSubmit}>
      <div className="form-row">
        <input
          className="input"
          placeholder="任务标题"
          value={form.title}
          onChange={(e) => set('title', e.target.value)}
        />
        <select
          className="input select"
          value={form.status}
          onChange={(e) => set('status', e.target.value as TaskStatus)}
        >
          {TASK_STATUSES.map((s) => (
            <option key={s} value={s}>
              {STATUS_LABEL[s]}
            </option>
          ))}
        </select>
        <select
          className="input select"
          value={form.priority}
          onChange={(e) => set('priority', e.target.value as TaskPriority)}
        >
          <option value="high">高优先级</option>
          <option value="medium">中优先级</option>
          <option value="low">低优先级</option>
        </select>
        <input
          className="input"
          type="date"
          value={form.dueAt ? new Date(form.dueAt).toISOString().slice(0, 10) : ''}
          onChange={(e) =>
            set('dueAt', e.target.value ? new Date(e.target.value + 'T00:00:00').getTime() : null)
          }
        />
        <button className="btn primary" type="submit" disabled={!form.title.trim()}>
          添加
        </button>
      </div>
      <textarea
        className="input textarea"
        placeholder="任务描述（可选）"
        value={form.description}
        onChange={(e) => set('description', e.target.value)}
      />
    </form>
  )
}

export default TaskForm
