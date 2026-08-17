#!/usr/bin/env bash
# 生成自签名证书（有效期 10 年），用于方案 A（无域名）
# 用法: bash deploy/gen_cert.sh <服务器公网IP>
set -e

IP="${1:?用法: bash deploy/gen_cert.sh <服务器公网IP>}"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/certs"
mkdir -p "$DIR"
cd "$DIR"

openssl req -x509 -nodes -newkey rsa:2048 -sha256 -days 3650 \
  -keyout key.pem -out cert.pem \
  -subj "/CN=${IP}" \
  -addext "subjectAltName=IP:${IP},DNS:localhost"

echo "证书已生成: $DIR/cert.pem"
echo "把 cert.pem 复制到每台客户端，并在同步设置中指定路径。"
