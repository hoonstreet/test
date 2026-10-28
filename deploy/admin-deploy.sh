#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
project_dir="$(cd -- "$script_dir/.." && pwd -P)"
runtime_dir="$project_dir/runtime/deployment"
request_file="$runtime_dir/deploy.request"
status_file="$runtime_dir/deploy.status"
lock_file="$runtime_dir/deploy.lock"
commit=""

mkdir -p -- "$runtime_dir"
umask 007

write_status() {
  local state="$1"
  local status_commit="${2:-}"
  local message="${3:-}"
  local temporary
  message="${message//$'\n'/ }"
  temporary="$(mktemp "$runtime_dir/.deploy-status.XXXXXX")"
  {
    printf 'state=%s\n' "$state"
    printf 'commit=%s\n' "$status_commit"
    printf 'message=%s\n' "$message"
    printf 'updatedAt=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  } > "$temporary"
  chmod 0660 "$temporary"
  mv -f -- "$temporary" "$status_file"
}

deployment_failed() {
  local exit_code=$?
  trap - ERR
  write_status "failed" "$commit" "Deployment failed (exit $exit_code). Check systemd logs."
  exit "$exit_code"
}
trap deployment_failed ERR

exec 9> "$lock_file"
if ! flock -n 9; then
  exit 0
fi

rm -f -- "$request_file"
cd -- "$project_dir"
commit="$(git rev-parse --short HEAD 2>/dev/null || true)"
write_status "running" "$commit" "Database backup and deployment are running."

if ! git diff --quiet || ! git diff --cached --quiet; then
  write_status "failed" "$commit" "Tracked server files have local changes; deployment was refused."
  exit 2
fi

backup_path="$(bash deploy/backup-db.sh)"
git fetch origin main
git pull --ff-only origin main
commit="$(git rev-parse --short HEAD)"
docker compose up -d --build app

timeout 180 bash -c 'until curl -fsS -o /dev/null http://127.0.0.1:8080/; do sleep 2; done'
write_status "succeeded" "$commit" "Deployment completed. Database backup: $(basename -- "$backup_path")"
trap - ERR
