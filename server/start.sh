#!/bin/sh
set -e

CERT=/app/certs/cert.pem
KEY=/app/certs/key.pem

if [ "$DISABLE_TLS" = "1" ]; then
  exec uvicorn main:app --host 0.0.0.0 --port "${PORT:-9443}"
fi

if [ ! -f "$CERT" ] || [ ! -f "$KEY" ]; then
  echo "证书不存在，请先运行 bash deploy/gen_cert.sh <IP>，或设置 DISABLE_TLS=1" >&2
  exit 1
fi

exec uvicorn main:app --host 0.0.0.0 --port "${PORT:-9443}" \
  --ssl-keyfile "$KEY" \
  --ssl-certfile "$CERT"
