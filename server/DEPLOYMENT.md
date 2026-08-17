# Mission 同步服务部署文档

mission-app 多设备同步服务（FastAPI + SQLite），部署到一台 Linux 服务器后，
所有客户端（Windows 桌面端、安卓端）即可离线记录、联网自动同步任务与项目。

## 架构

```
Windows 客户端 ──HTTPS──▶ 同步服务 (uvicorn :9443) ──▶ SQLite (data/sync.db)
Android 客户端  ──HTTPS──▶     FastAPI 应用容器
```

- 服务只暴露一个 HTTPS 端口 `9443`。
- **按用户隔离**：每个用户通过注册接口获得独立 token，数据互不可见。
- **冲突策略**：Last-write-wins（按 `updatedAt` 时间戳，后保存的覆盖先保存的），配合删除墓碑。
- 客户端首次同步时会把本地已有任务推送到服务器并合并，**已有任务不会丢失**。

## API 一览

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| GET | `/api/v1/health` | 健康检查，返回 `{"ok": true}` | 无 |
| POST | `/api/v1/register` | 注册用户，返回 `{token, user}` | 无 |
| POST | `/api/v1/sync` | 全量同步（push + pull），返回合并后的状态 | Bearer token |

`POST /api/v1/sync` 请求体：

```json
{
  "tasks": [{ "id": "...", "title": "...", "status": "in_progress", "updatedAt": 1690000000000, "..." }],
  "projects": [{ "id": "...", "name": "...", "updatedAt": 1690000000000, "..." }],
  "deleted": [{ "kind": "task", "id": "...", "updatedAt": 1690000000000 }]
}
```

响应体：

```json
{
  "tasks": [...],
  "projects": [...],
  "tombstones": [{ "kind": "task", "id": "...", "updatedAt": 1690000000000 }]
}
```

> 安卓端可直接复用这套接口，无需改动服务端。

## 方案 A：Docker Compose + 自签名证书（无域名，推荐）

### 步骤 1：上传代码

```bash
scp -r server/ root@<你的服务器IP>:/opt/mission-server
cd /opt/mission-server
```

### 步骤 2：配置并启动

```bash
cp .env.example .env
bash deploy/deploy.sh <你的服务器公网IP>
```

`deploy.sh` 会依次：安装 Docker/Compose → 生成自签证书（10 年）→ 构建启动。

### 步骤 3：验证

```bash
docker compose ps
curl -k https://<你的服务器IP>:9443/api/v1/health
# => {"ok":true}
```

### 步骤 4：注册用户

```bash
curl -k -X POST https://<你的服务器IP>:9443/api/v1/register \
  -H 'Content-Type: application/json' -d '{"name":"你的用户名"}'
```

返回的 `token` 即为该用户的同步令牌（多设备填同一个 token 即共享同一份数据）。

### 步骤 5：配置客户端

1. 把 `server/certs/cert.pem` 复制到客户端（任一路径，如 `C:\mission\cert.pem`）。
2. 桌面端点「☁ 同步设置」，填写服务器地址、token、证书路径。
3. 保存后观察状态栏变为「已同步 HH:MM:SS」即成功。

## 方案 B：域名 + 公网证书（反代）

有域名时用 Caddy/Nginx 反代，客户端无需携带证书文件：

```bash
# Caddy 示例
# sudo apt install -y caddy
# 在 /etc/caddy/Caddyfile 中反向代理到 127.0.0.1:9443，并开启 TLS
```

客户端服务器地址填 `https://你的域名`，证书路径留空即可。

## 运维

### 数据备份

```bash
docker compose stop mission-sync
cp data/sync.db backup/sync-$(date +%F).db
docker compose start mission-sync
```

### 升级

```bash
docker compose up -d --build --pull always
```

### 本地调试（关闭 TLS）

```powershell
$env:DB_PATH="data\sync.db"; $env:DISABLE_TLS="1"; uvicorn main:app --port 9443
```

客户端服务器地址填 `http://127.0.0.1:9443`，证书路径留空。

### 常见问题

| 现象 | 排查 |
|---|---|
| 客户端显示「离线，等待重连」 | 安全组是否放行 `9443`；地址是否 `https://` + 端口；容器是否 Running；自签证书路径是否正确 |
| 返回 `401 令牌无效` | token 错误，重新注册或用正确的 token |
| 证书无效 | 客户端证书路径填错；重新拷贝 `certs/cert.pem` |
| 端口冲突 | 修改 `.env` 的 `PORT` 并重新 `docker compose up -d` |

### 安全建议

- 证书私钥 `key.pem` 不要外传，客户端只需要 `cert.pem`。
- 定期备份 `data/sync.db`。
- token 即用户凭据，妥善保管；服务器哈希存储，不泄露明文。
