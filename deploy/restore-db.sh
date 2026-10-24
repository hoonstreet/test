#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
project_dir="$(cd -- "$script_dir/.." && pwd -P)"
backup_dir="${BACKUP_DIR:-$project_dir/backups}"

if (( $# != 1 )); then
  echo "Usage: bash deploy/restore-db.sh backups/isahaedream_YYYYMMDDTHHMMSSZ.dump" >&2
  exit 1
fi

for command_name in docker realpath sha256sum; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Required command not found: $command_name" >&2
    exit 1
  fi
done

cd "$project_dir"
if [[ ! -f .env ]]; then
  echo "REFUSED: $project_dir/.env does not exist." >&2
  exit 1
fi

backup_dir="$(realpath -- "$backup_dir")"
restore_path="$(realpath -- "$1")"
if [[ "$restore_path" != "$backup_dir"/isahaedream_*.dump || ! -f "$restore_path" ]]; then
  echo "REFUSED: select an isahaedream_*.dump file inside $backup_dir" >&2
  exit 1
fi

if [[ -f "$restore_path.sha256" ]]; then
  (cd -- "$backup_dir" && sha256sum --check --status "$(basename -- "$restore_path.sha256")") || {
    echo "REFUSED: backup checksum validation failed." >&2
    exit 1
  }
fi

docker compose exec -T db pg_restore --list < "$restore_path" >/dev/null || {
  echo "REFUSED: PostgreSQL cannot read this backup archive." >&2
  exit 1
}

echo "WARNING: this replaces the current isahaedream database with:" >&2
echo "  $restore_path" >&2
read -r -p "Type RESTORE to continue: " confirmation
if [[ "$confirmation" != "RESTORE" ]]; then
  echo "Restore cancelled." >&2
  exit 1
fi

safety_backup="$(bash "$script_dir/backup-db.sh")"
echo "Pre-restore safety backup: $safety_backup" >&2

app_stopped=false
restart_app() {
  if [[ "$app_stopped" == true ]]; then
    docker compose up -d app >/dev/null
  fi
}
trap restart_app EXIT

docker compose stop app
app_stopped=true
docker compose exec -T db dropdb --username=isahaedream --if-exists --force isahaedream
docker compose exec -T db createdb --username=isahaedream --owner=isahaedream isahaedream
docker compose exec -T db pg_restore \
  --username=isahaedream \
  --dbname=isahaedream \
  --no-owner \
  --no-privileges < "$restore_path"
docker compose up -d app
app_stopped=false
trap - EXIT

echo "Database restore completed. Safety backup: $safety_backup"
