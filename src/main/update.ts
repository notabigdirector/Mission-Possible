import { dialog } from 'electron'
import { autoUpdater } from 'electron-updater'
import { is } from '@electron-toolkit/utils'

export class AppUpdater {
  constructor() {
    autoUpdater.logger = null
    autoUpdater.autoDownload = true

    autoUpdater.on('error', (error) => {
      console.error('更新检查失败:', error)
    })

    autoUpdater.on('update-available', (info) => {
      console.log(`发现新版本 ${info.version}，开始下载...`)
    })

    autoUpdater.on('update-not-available', () => {
      console.log('当前已是最新版本')
    })

    autoUpdater.on('update-downloaded', (info) => {
      dialog
        .showMessageBox({
          type: 'info',
          title: '应用更新',
          message: `新版本 ${info.version} 已下载完成`,
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
    })

    // 仅在打包后的生产环境中检查更新
    if (!is.dev) {
      autoUpdater.checkForUpdatesAndNotify()
    }
  }
}
