# mission-app 任务管理桌面应用

一个基于 Electron + React + TypeScript 的本地任务管理桌面客户端，支持任务的新增、编辑、删除、完成状态切换、优先级标记、截止日期与分类筛选。数据通过 Electron 主进程持久化到本地 JSON 文件，无需服务器即可使用，并内置基于 GitHub Releases 的自动更新能力。

## 功能

- 任务增删改查：标题、描述、优先级（高/中/低）、截止日期
- 完成状态切换，已完成任务划线显示
- 筛选视图：全部 / 进行中 / 已完成
- 数据本地持久化（自动保存，升级不丢失）

## 技术架构

```
src/
├── main/          # 主进程（后端）：窗口、数据持久化、IPC、自动更新
│   ├── index.ts       # 应用入口
│   ├── task-store.ts  # JSON 文件存储（位于系统 userData 目录）
│   ├── ipc.ts         # IPC 接口注册（增删改查）
│   └── update.ts      # electron-updater 自动更新逻辑
├── preload/       # 预加载脚本：通过 contextBridge 暴露类型化 API
├── renderer/      # 渲染进程（前端）：React + TypeScript
│   └── src/
│       ├── App.tsx               # 主界面：状态管理 + 筛选
│       └── components/           # TaskForm / TaskItem
└── shared/        # 主进程与渲染进程共享的类型定义
```

数据流：渲染进程调用 `window.api.tasks.*` → IPC（invoke/handle）→ 主进程 `TaskStore` 读写 `userData/tasks.json`。

## 环境要求

- Node.js 20+（推荐 LTS）

## 本地开发

```bash
npm install        # 安装依赖
npm run dev        # 开发模式（HMR）
```

> 网络无法直连 GitHub 时，安装 Electron 需使用镜像：
> ```powershell
> $env:ELECTRON_MIRROR="https://npmmirror.com/mirrors/electron/"
> npm install
> ```

### 代码检查与构建

```bash
npm run typecheck          # TypeScript 类型检查
npm run lint               # ESLint 检查
npm run build:win          # 构建 Windows 安装包（dist/ 目录）
```

## 数据存储位置

任务数据保存在系统用户数据目录：

- Windows：`%APPDATA%/mission-app/tasks.json`
- macOS：`~/Library/Application Support/mission-app/tasks.json`
- Linux：`~/.config/mission-app/tasks.json`

卸载重装、升级版本均不会删除数据。

## 发布与自动更新

应用使用 [electron-updater](https://www.electron.build/auto-update) 通过 GitHub Releases 自动更新。

### 首次发布配置

1. 在 `electron-builder.yml` 和 `dev-app-update.yml` 中替换占位符：
   ```yaml
   publish:
     provider: github
     owner: YOUR_GITHUB_USERNAME
     repo: YOUR_REPOSITORY_NAME
   ```
2. 在 GitHub 创建同名仓库并推送代码：
   ```bash
   git remote add origin git@github.com:<用户名>/<仓库>.git
   git push -u origin main
   ```
3. 在 GitHub 仓库 **Settings → Secrets and variables → Actions** 添加 `GH_TOKEN`（Personal access token，勾选 `repo` 权限），用于工作流上传 Release 产物。

### 发布新版本（推荐：CI 自动发布）

提交改动后打上版本号 tag 并推送：

```bash
git add . && git commit -m "feat: 新功能描述"
git tag v1.1.0
git push origin main --tags
```

`.github/workflows/release.yml` 会自动执行：安装依赖 → 从 tag 设置版本号 → 构建 → 上传 `setup.exe` + `latest.yml` 到 GitHub Releases。

### 手动发布（不使用 CI）

```bash
npm run release:win        # 需先设置 GH_TOKEN 环境变量
```

`npm run release:win` 会在本机构建并直接发布到 GitHub Releases。

### 用户端升级流程

已安装的用户启动应用时自动检查更新 → 发现新版本后台下载 → 弹窗提示"立即重启/稍后" → 重启完成升级。若未检测到新版本，请确认：

- tag 版本号高于用户当前安装版本（tag `v1.1.0` → 应用版本 `1.1.0`）
- 最新 Release 的 `latest.yml` 与 `setup.exe` 已正常上传
- 用户机器可访问 `github.com`

## 注意事项

- **代码签名**：未签名的安装包在 Windows 上会触发 SmartScreen 警告，静默自动更新同样受影响。正式分发建议购买代码签名证书。
- **更新源网络**：自动更新默认从 GitHub Releases 下载，国内用户访问可能较慢；如需面向国内用户，可将 `publish` 改为 `provider: generic` 指向自建静态服务器。

## 项目配置

- 打包配置：`electron-builder.yml`
- 自动更新开发调试配置：`dev-app-update.yml`
- 发布工作流：`.github/workflows/release.yml`
