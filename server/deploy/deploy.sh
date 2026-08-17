#!/usr/bin/env bash
# 一键部署到 Linux 服务器（Docker Compose + 自签名证书 + 自动生成随机 .env）
# 用法: bash deploy.sh <服务器公网IP>
set -e

IP="${1:?用法: bash deploy.sh <服务器公网IP>}"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# 1. 安装 Docker / Compose（未安装时）
if ! command -v docker >/dev/null 2>&1; then
  echo "==> 安装 Docker..."
  curl -fsSL https://get.docker.com | sh
  systemctl enable --now docker || true
fi
if ! docker compose version >/dev/null 2>&1; then
  echo "==> 安装 Docker Compose 插件..."
  apt-get update && apt-get install -y docker-compose-plugin || \
  docker plugin install docker-compose >/dev/null 2>&1 || true
fi

cd "$DIR"

# 2. 生成证书
echo "==> 生成自签名证书..."
bash deploy/gen_cert.sh "$IP"

# 3. 准备 .env
if [ ! -f .env ]; then
  echo "==> 生成 .env..."
  cp .env.example .env
fi

# 4. 构建并启动
echo "==> 构建并启动..."
docker compose up -d --build

echo ""
echo "部署完成: https://${IP}:9443"
echo "健康检查: curl -k https://${IP}:9443/api/v1/health"
echo "客户端注册用户: curl -k -X POST https://${IP}:9443/api/v1/register -H 'Content-Type: application/json' -d '{\"name\":\"你的用户名\"}'"
echo "把 server/certs/cert.pem 复制到各客户端，并在同步设置中指定证书路径。"
