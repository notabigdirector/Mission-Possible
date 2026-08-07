import { useState } from 'react'
import type { Project, ProjectInput, ProjectUpdate } from '../../../shared/types'

interface Props {
  projects: Project[]
  onCreate: (input: ProjectInput) => void
  onUpdate: (id: string, patch: ProjectUpdate) => void
  onRemove: (id: string) => void
  onClose: () => void
}

function ProjectRow({
  project,
  onSave,
  onRemove
}: {
  project: Project
  onSave: (id: string, patch: ProjectUpdate) => void
  onRemove: (id: string) => void
}): React.JSX.Element {
  const [name, setName] = useState(project.name)
  const [priority, setPriority] = useState(String(project.priority))

  const dirty = name.trim() !== project.name || Number(priority) !== project.priority

  return (
    <div className="project-row">
      <input
        className="input"
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="项目名称"
      />
      <input
        className="input project-priority-input"
        type="number"
        value={priority}
        onChange={(e) => setPriority(e.target.value)}
        placeholder="优先级"
      />
      <button
        className="btn"
        onClick={() => onSave(project.id, { name: name.trim(), priority: Number(priority) || 0 })}
        disabled={!name.trim() || !dirty}
      >
        保存
      </button>
      <button className="btn danger" onClick={() => onRemove(project.id)}>
        删除
      </button>
    </div>
  )
}

function ProjectManager({ projects, onCreate, onUpdate, onRemove, onClose }: Props): React.JSX.Element {
  const [name, setName] = useState('')
  const [priority, setPriority] = useState('0')

  const add = (): void => {
    if (!name.trim()) return
    onCreate({ name: name.trim(), priority: Number(priority) || 0 })
    setName('')
    setPriority('0')
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>项目管理</h2>
        <div className="project-list">
          {projects.length === 0 ? (
            <p className="project-empty">暂无项目，添加一个吧</p>
          ) : (
            projects.map((p) => (
              <ProjectRow key={p.id} project={p} onSave={onUpdate} onRemove={onRemove} />
            ))
          )}
        </div>
        <div className="project-add">
          <input
            className="input"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="新项目名称"
            onKeyDown={(e) => {
              if (e.key === 'Enter') add()
            }}
          />
          <input
            className="input project-priority-input"
            type="number"
            value={priority}
            onChange={(e) => setPriority(e.target.value)}
            placeholder="优先级"
          />
          <button className="btn primary" onClick={add} disabled={!name.trim()}>
            添加
          </button>
        </div>
        <div className="modal-actions">
          <button className="btn" onClick={onClose}>
            关闭
          </button>
        </div>
      </div>
    </div>
  )
}

export default ProjectManager
