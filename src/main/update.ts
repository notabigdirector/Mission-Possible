import { dialog } from 'electron'
import { autoUpdater } from 'electron-updater'
import { is } from '@electron-toolkit/utils'

export class AppUpdater {
  private manual = false

  constructor() {
    autoUpdater.logger = null
    autoUpdater.autoDownload = false

    autoUpdater.on('error', (error) => {
      console.error('更新检查失败:', error)
      if (this.manual) {
        this.manual = false
        dialog.showMessageBox({
          type: 'error',
          title: '检查更新失败',
          message: '无法检查更新，请稍后再试',
          detail: error.message
        })
      }
    })

    autoUpdater.on('update-available', (info) => {
      this.manual = false
      this.promptDownload(info.version)
    })

    autoUpdater.on('update-not-available', () => {
      if (this.manual) {
        this.manual = false
        dialog.showMessageBox({
          type: 'info',
          title: '检查更新',
          message: '当前已是最新版本'
        })
      }
    })

    autoUpdater.on('update-downloaded', (info) => {
      this.promptInstall(info.version)
    })

    // 仅在打包后的生产环境中自动检查更新
    if (!is.dev) {
      this.checkForUpdates()
    }
  }

  checkForUpdates(manual = false): void {
    this.manual = manual
    autoUpdater.checkForUpdates()
  }

  private promptDownload(version: string): void {
    dialog
      .showMessageBox({
        type: 'info',
        title: '发现新版本',
        message: `发现新版本 v${version}`,
        detail: '是否立即下载并更新？',
        buttons: ['立即更新', '稍后'],
        defaultId: 0,
        cancelId: 1
      })
      .then((result) => {
        if (result.response === 0) {
          autoUpdater.downloadUpdate()
        }
      })
  }

  private promptInstall(version: string): void {
    dialog
      .showMessageBox({
        type: 'info',
        title: '更新完成',
        message: `新版本 v${version} 已下载完成`,
        detail: '重启应用即可完成更新',
        buttons: ['立即重启', '稍后再说'],
        defaultId: 0,
        cancelId: 1
      })
      .then((result) => {
        if (result.response === 0) {
          autoUpdater.quitAndInstall()
        }
      })
  }
}
