#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
project_dir="$(cd -- "$script_dir/.." && pwd -P)"
backup_dir="${BACKUP_DIR:-$project_dir/backups}"

for command_name in docker sha256sum; do
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

if ! docker compose ps --status running --services | grep -Fxq db; then
  echo "REFUSED: the database container is not running." >&2
  exit 1
fi

mkdir -p -- "$backup_dir"
chmod 700 "$backup_dir"
backup_dir="$(cd -- "$backup_dir" && pwd -P)"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
final_path="$backup_dir/isahaedream_${timestamp}.dump"
temporary_path="$backup_dir/.isahaedream_${timestamp}_$$.tmp"

cleanup() {
  if [[ -n "${temporary_path:-}" && "$temporary_path" == "$backup_dir"/.isahaedream_*.tmp ]]; then
    rm -f -- "$temporary_path"
  fi
}
trap cleanup EXIT

if [[ -e "$final_path" ]]; then
  echo "REFUSED: backup target already exists: $final_path" >&2
  exit 1
fi

docker compose exec -T db pg_dump \
  --username=isahaedream \
  --dbname=isahaedream \
  --format=custom \
  --no-owner \
  --no-privileges > "$temporary_path"

if [[ ! -s "$temporary_path" ]]; then
  echo "Backup validation failed: dump file is empty." >&2
  exit 1
fi

docker compose exec -T db pg_restore --list < "$temporary_path" >/dev/null
chmod 600 "$temporary_path"
mv -- "$temporary_path" "$final_path"
(cd -- "$backup_dir" && sha256sum "$(basename -- "$final_path")") > "$final_path.sha256"
chmod 600 "$final_path.sha256"
trap - EXIT

echo "Database backup verified: $final_path" >&2
printf '%s\n' "$final_path"
