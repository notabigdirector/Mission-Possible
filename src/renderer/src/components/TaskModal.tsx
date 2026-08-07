import { useState } from 'react'
import type {
  Project,
  Task,
  TaskInput,
  TaskPriority,
  TaskStatus,
  TaskUpdate
} from '../../../shared/types'
import { TASK_STATUSES, STATUS_LABEL } from '../../../shared/types'

interface Props {
  task: Task
  parents: Task[]
  projects: Project[]
  onSave: (id: string, patch: TaskUpdate) => void
  onClose: () => void
}

function TaskModal({ task, parents, projects, onSave, onClose }: Props): React.JSX.Element {
  const [draft, setDraft] = useState<TaskInput>({
    parentId: task.parentId,
    projectId: task.projectId,
    title: task.title,
    description: task.description,
    status: task.status,
    priority: task.priority,
    dueAt: task.dueAt
  })

  const save = (): void => {
    if (!draft.title.trim()) return
    onSave(task.id, { ...draft, title: draft.title.trim() })
    onClose()
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>任务详情</h2>
        <label className="modal-field">
          <span>父任务</span>
          <select
            className="input select"
            value={draft.parentId ?? ''}
            onChange={(e) => {
              const parentId = e.target.value ? e.target.value : null
              const parent = parentId ? parents.find((p) => p.id === parentId) : undefined
              setDraft((d) => ({
                ...d,
                parentId,
                projectId: parent ? parent.projectId : d.projectId
              }))
            }}
          >
            <option value="">主任务（无上级）</option>
            {parents.map((p) => (
              <option key={p.id} value={p.id}>
                {p.title}
              </option>
            ))}
          </select>
        </label>
        <label className="modal-field">
          <span>项目</span>
          <select
            className="input select"
            value={draft.projectId ?? ''}
            onChange={(e) =>
              setDraft((d) => ({ ...d, projectId: e.target.value ? e.target.value : null }))
            }
            disabled={!!draft.parentId}
          >
            <option value="">无项目</option>
            {projects.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
        </label>
        <label className="modal-field">
          <span>标题</span>
          <input
            className="input"
            value={draft.title}
            onChange={(e) => setDraft((d) => ({ ...d, title: e.target.value }))}
          />
        </label>
        <label className="modal-field">
          <span>描述</span>
          <textarea
            className="input textarea"
            value={draft.description}
            onChange={(e) => setDraft((d) => ({ ...d, description: e.target.value }))}
          />
        </label>
        <label className="modal-field">
          <span>状态</span>
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
        </label>
        <label className="modal-field">
          <span>优先级</span>
          <select
            className="input select"
            value={draft.priority}
            onChange={(e) => setDraft((d) => ({ ...d, priority: e.target.value as TaskPriority }))}
          >
            <option value="high">高优先级</option>
            <option value="medium">中优先级</option>
            <option value="low">低优先级</option>
          </select>
        </label>
        <label className="modal-field">
          <span>截止日期</span>
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
        </label>
        <div className="modal-meta">
          <span>创建于 {new Date(task.createdAt).toLocaleString()}</span>
          <span>更新于 {new Date(task.updatedAt).toLocaleString()}</span>
        </div>
        <div className="modal-actions">
          <button className="btn primary" onClick={save} disabled={!draft.title.trim()}>
            保存
          </button>
          <button className="btn" onClick={onClose}>
            取消
          </button>
        </div>
      </div>
    </div>
  )
}

export default TaskModal
