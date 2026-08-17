import { useEffect, useState } from 'react'
import type { SyncConfig, SyncStatus } from '../../../shared/types'

interface Props {
  config: SyncConfig
  status: SyncStatus
  onSave: (config: SyncConfig) => Promise<void>
  onClose: () => void
}

function SyncSettings({ config, status, onSave, onClose }: Props): React.JSX.Element {
  const [serverUrl, setServerUrl] = useState(config.serverUrl)
  const [token, setToken] = useState(config.token)
  const [certPath, setCertPath] = useState(config.certPath)
  const [userName, setUserName] = useState(config.userName)
  const [registerName, setRegisterName] = useState('')
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [saveOk, setSaveOk] = useState(false)

  useEffect(() => {
    const t = setTimeout(() => setSaveOk(false), 2000)
    return () => clearTimeout(t)
  }, [saveOk])

  const save = async (): Promise<void> => {
    setBusy(true)
    setNotice(null)
    try {
      await onSave({ serverUrl: serverUrl.trim(), token: token.trim(), certPath: certPath.trim(), userName: userName.trim() })
      setSaveOk(true)
    } catch (err) {
      setNotice(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  const register = async (): Promise<void> => {
    if (!registerName.trim()) {
      setNotice('请输入用户名')
      return
    }
    setBusy(true)
    setNotice(null)
    try {
      const result = await window.api.sync.register(registerName.trim())
      setToken(result.token)
      setUserName(result.user.name)
      setRegisterName('')
      setNotice('注册成功，token 已填入，请点击「保存」')
    } catch (err) {
      setNotice(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  const syncNow = async (): Promise<void> => {
    setBusy(true)
    setNotice(null)
    try {
      await window.api.sync.now()
    } catch (err) {
      setNotice(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>☁ 同步设置</h2>
        <div className="sync-status">
          状态：
          {status.state === 'ok' && <span className="sync-state ok">已同步</span>}
          {status.state === 'syncing' && <span className="sync-state syncing">同步中…</span>}
          {status.state === 'error' && <span className="sync-state error">错误</span>}
          {status.state === 'idle' && <span className="sync-state idle">未配置</span>}
          {status.lastSyncAt && (
            <span className="sync-last">上次同步 {new Date(status.lastSyncAt).toLocaleTimeString()}</span>
          )}
          {status.state === 'error' && status.message && (
            <span className="sync-msg">{status.message}</span>
          )}
        </div>
        <label className="modal-field">
          服务器地址
          <input
            className="input"
            value={serverUrl}
            onChange={(e) => setServerUrl(e.target.value)}
            placeholder="https://服务器IP:9443"
          />
        </label>
        <label className="modal-field">
          同步 Token
          <input
            className="input"
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder="用户注册后获得的 token"
          />
        </label>
        <label className="modal-field">
          用户名（可选，仅显示）
          <input
            className="input"
            value={userName}
            onChange={(e) => setUserName(e.target.value)}
            placeholder="你的名字"
          />
        </label>
        <label className="modal-field">
          服务器证书路径（自签证书场景填，公网域名留空）
          <input
            className="input"
            value={certPath}
            onChange={(e) => setCertPath(e.target.value)}
            placeholder="C:\mission\cert.pem"
          />
        </label>
        {notice && <div className="sync-notice">{notice}</div>}
        <div className="modal-actions">
          <button className="btn" onClick={syncNow} disabled={busy}>
            立即同步
          </button>
          <button className="btn" onClick={save} disabled={busy}>
            {saveOk ? '已保存 ✓' : '保存'}
          </button>
          <button className="btn" onClick={onClose}>
            关闭
          </button>
        </div>
        <hr className="sync-divider" />
        <div className="sync-register">
          <span className="sync-register-tip">还没有账号？注册一个（返回的 token 自动填入）</span>
          <div className="sync-register-row">
            <input
              className="input"
              value={registerName}
              onChange={(e) => setRegisterName(e.target.value)}
              placeholder="新用户名"
            />
            <button className="btn primary" onClick={register} disabled={busy}>
              注册用户
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default SyncSettings
