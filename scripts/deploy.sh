#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
DEPLOY_DIR="${ROOT_DIR}/deploy"
ENV_FILE="${ENV_FILE:-${DEPLOY_DIR}/.env}"
COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.prod.yml"

log() {
  printf '\n[%s] %s\n' "$(date '+%F %T')" "$*"
}

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" "$@"
  elif command -v docker-compose >/dev/null 2>&1; then
    docker-compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" "$@"
  else
    fail "missing docker compose plugin or docker-compose"
  fi
}

random_secret() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -base64 24 | tr -d '\n'
  elif command -v sha256sum >/dev/null 2>&1; then
    date "+%s%N" | sha256sum | awk '{print $1}'
  else
    date "+%s%N" | shasum -a 256 | awk '{print $1}'
  fi
}

create_env_if_missing() {
  if [ -f "${ENV_FILE}" ]; then
    return
  fi

  mkdir -p "${DEPLOY_DIR}"
  local server_name="${SERVER_NAME:-localhost}"
  local mini_api_base_url="${MINI_API_BASE_URL:-http://localhost/api}"

  cat > "${ENV_FILE}" <<EOF
COMPOSE_PROJECT_NAME=zhiyi-erp

AUTO_GIT_PULL=true
GIT_REMOTE=origin
GIT_BRANCH=main

SERVER_NAME=${server_name}
HTTP_PORT=${HTTP_PORT:-80}
CLIENT_MAX_BODY_SIZE=${CLIENT_MAX_BODY_SIZE:-50m}

MYSQL_ROOT_PASSWORD=$(random_secret)
MYSQL_DATABASE=zhiyi_erp
MYSQL_USER=zhiyi
MYSQL_PASSWORD=$(random_secret)

REDIS_PASSWORD=$(random_secret)

JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC

BUILD_MINI=true
MINI_API_BASE_URL=${mini_api_base_url}
EOF

  log "created ${ENV_FILE}. Review SERVER_NAME and MINI_API_BASE_URL before public release."
}

env_value() {
  local key="$1"
  local default_value="${2:-}"

  if [ ! -f "${ENV_FILE}" ]; then
    printf '%s' "${default_value}"
    return
  fi

  local value
  value="$(awk -F= -v k="${key}" '$1 == k { sub(/^[^=]*=/, ""); gsub(/^"|"$/, ""); print; exit }' "${ENV_FILE}")"
  if [ -n "${value}" ]; then
    printf '%s' "${value}"
  else
    printf '%s' "${default_value}"
  fi
}

check_base_tools() {
  need_cmd docker
  docker info >/dev/null 2>&1 || fail "Docker daemon is not running or current user cannot access it"
  compose version >/dev/null
}

check_git_clean() {
  local status
  status="$(git -C "${ROOT_DIR}" status --porcelain)"
  if [ -n "${status}" ]; then
    fail "working tree is not clean. Commit, stash, or disable AUTO_GIT_PULL before deploying."
  fi
}

pull_code() {
  local auto_pull
  auto_pull="$(env_value AUTO_GIT_PULL true)"
  if [ "${auto_pull}" != "true" ]; then
    log "skipping git pull because AUTO_GIT_PULL=${auto_pull}"
    return
  fi

  local remote branch
  remote="$(env_value GIT_REMOTE origin)"
  branch="$(env_value GIT_BRANCH main)"

  need_cmd git
  check_git_clean
  log "pulling latest code from ${remote}/${branch}"
  git -C "${ROOT_DIR}" fetch "${remote}" "${branch}"
  git -C "${ROOT_DIR}" pull --ff-only "${remote}" "${branch}"
}

build_backend() {
  log "building backend jar"
  (cd "${ROOT_DIR}/zhiyi-server" && ./mvnw -q -DskipTests clean package)
}

build_web() {
  log "building web frontend"
  (cd "${ROOT_DIR}/zhiyi-web" && npm ci && npm run build)
}

build_mini() {
  local build_mini
  build_mini="$(env_value BUILD_MINI true)"
  if [ "${build_mini}" != "true" ]; then
    log "skipping mini program build because BUILD_MINI=${build_mini}"
    return
  fi

  local mini_api_base_url
  mini_api_base_url="$(env_value MINI_API_BASE_URL http://localhost/api)"

  log "building mini program with API base URL: ${mini_api_base_url}"
  (cd "${ROOT_DIR}/zhiyi-mini" && npm ci && TARO_APP_API_BASE_URL="${mini_api_base_url}" ./node_modules/.bin/taro build --type weapp --no-check)
}

up() {
  log "starting docker services"
  compose up -d --build
  compose ps
}

up_backend() {
  log "starting backend service"
  compose up -d --build backend
  compose ps backend
}

up_web() {
  log "starting nginx web service"
  compose up -d --no-deps nginx
  compose ps nginx
}

deploy() {
  create_env_if_missing
  pull_code
  check_base_tools
  need_cmd npm
  build_web
  build_backend
  build_mini
  up
  log "deployment finished. Web entry: http://$(env_value SERVER_NAME localhost):$(env_value HTTP_PORT 80)"
}

deploy_backend() {
  create_env_if_missing
  pull_code
  check_base_tools
  build_backend
  up_backend
  log "backend deployment finished"
}

deploy_web() {
  create_env_if_missing
  pull_code
  check_base_tools
  need_cmd npm
  build_web
  up_web
  log "web deployment finished. Web entry: http://$(env_value SERVER_NAME localhost):$(env_value HTTP_PORT 80)"
}

deploy_mini() {
  create_env_if_missing
  pull_code
  need_cmd npm
  build_mini
  log "mini program build finished. Upload zhiyi-mini/dist with WeChat DevTools."
}

backup_mysql() {
  create_env_if_missing
  check_base_tools
  local backup_dir="${DEPLOY_DIR}/backups"
  local file="${backup_dir}/zhiyi_erp_$(date '+%Y%m%d_%H%M%S').sql.gz"
  mkdir -p "${backup_dir}"
  log "creating MySQL backup: ${file}"
  compose exec -T mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' | gzip > "${file}"
}

usage() {
  cat <<'EOF'
Usage: scripts/deploy.sh [command]

Commands:
  deploy          Build web/backend/mini and start Docker services. Default.
  deploy:web      Pull, build web, and refresh nginx.
  deploy:backend  Pull, build backend jar/image, and restart backend.
  deploy:mini     Pull and build mini program dist.
  pull            Pull latest code using AUTO_GIT_PULL/GIT_REMOTE/GIT_BRANCH config.
  build           Build web/backend/mini only.
  build:web       Build web only.
  build:backend   Build backend only.
  build:mini      Build mini program only.
  mini            Alias of build:mini.
  up              Start Docker services with build.
  down            Stop Docker services.
  restart         Restart Docker services.
  ps              Show Docker service status.
  logs            Follow logs. Optional service name: scripts/deploy.sh logs backend
  backup          Dump MySQL backup to deploy/backups/.
EOF
}

command_name="${1:-deploy}"

case "${command_name}" in
  deploy)
    deploy
    ;;
  deploy:web|web)
    deploy_web
    ;;
  deploy:backend|backend)
    deploy_backend
    ;;
  deploy:mini)
    deploy_mini
    ;;
  pull)
    create_env_if_missing
    pull_code
    ;;
  build)
    create_env_if_missing
    need_cmd npm
    build_web
    build_backend
    build_mini
    ;;
  build:web)
    create_env_if_missing
    need_cmd npm
    build_web
    ;;
  build:backend)
    create_env_if_missing
    build_backend
    ;;
  build:mini|mini)
    create_env_if_missing
    need_cmd npm
    build_mini
    ;;
  up)
    create_env_if_missing
    check_base_tools
    up
    ;;
  down)
    create_env_if_missing
    check_base_tools
    compose down
    ;;
  restart)
    create_env_if_missing
    check_base_tools
    compose restart
    ;;
  ps)
    create_env_if_missing
    check_base_tools
    compose ps
    ;;
  logs)
    create_env_if_missing
    check_base_tools
    service_name="${2:-backend}"
    compose logs -f "${service_name}"
    ;;
  backup)
    backup_mysql
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    usage
    fail "unknown command: ${command_name}"
    ;;
esac
