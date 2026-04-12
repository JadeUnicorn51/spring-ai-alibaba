#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose-server-hostdb.yaml"
ENV_FILE="${SCRIPT_DIR}/.env.server"
ENV_EXAMPLE_FILE="${SCRIPT_DIR}/.env.server.example"

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  echo "[ERROR] docker compose / docker-compose not found."
  exit 1
fi

compose() {
  "${COMPOSE_CMD[@]}" --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" "$@"
}

ensure_env_file() {
  if [[ -f "${ENV_FILE}" ]]; then
    return 0
  fi

  cp "${ENV_EXAMPLE_FILE}" "${ENV_FILE}"
  echo "[WARN] ${ENV_FILE} not found. Created from example."
  echo "[WARN] Please edit MYSQL/REDIS credentials in ${ENV_FILE}, then rerun."
  exit 1
}

up() {
  ensure_env_file
  compose up -d \
    elasticsearch \
    kibana \
    elasticsearch-init \
    loongcollector \
    nacos \
    rmq-namesrv \
    rmq-broker \
    rmq-proxy \
    rmq-init-topic \
    backend \
    frontend
  echo "[INFO] Services started."
  echo "[INFO] MySQL migration SQL is not auto executed (run manually as requested)."
}

down() {
  ensure_env_file
  compose down
}

status() {
  ensure_env_file
  compose ps
}

logs() {
  ensure_env_file
  if [[ $# -gt 0 ]]; then
    compose logs -f "$1"
  else
    compose logs -f
  fi
}

init_es() {
  ensure_env_file
  compose up -d elasticsearch
  compose run --rm elasticsearch-init
}

usage() {
  cat <<EOF
Usage: $(basename "$0") <command> [service]

Commands:
  up         Start server deployment (without MySQL/Redis containers)
  down       Stop and remove containers in this deployment
  ps         Show container status
  logs       Show logs (optional service name)
  init-es    Run Elasticsearch init script explicitly
EOF
}

main() {
  local cmd="${1:-}"
  case "${cmd}" in
    up)
      up
      ;;
    down)
      down
      ;;
    ps)
      status
      ;;
    logs)
      shift || true
      logs "${1:-}"
      ;;
    init-es)
      init_es
      ;;
    *)
      usage
      exit 1
      ;;
  esac
}

main "$@"

