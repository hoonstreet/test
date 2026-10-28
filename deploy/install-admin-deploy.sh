#!/usr/bin/env bash
set -Eeuo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
project_dir="$(cd -- "$script_dir/.." && pwd -P)"
expected_dir="/home/ubuntu/isahaedream-site"

if [[ "$project_dir" != "$expected_dir" ]]; then
  echo "REFUSED: expected project directory $expected_dir, found $project_dir" >&2
  exit 1
fi

install -d -m 0770 "$project_dir/runtime/deployment"
if [[ ! -f "$project_dir/runtime/deployment/deploy.status" ]]; then
  {
    echo 'state=idle'
    echo 'commit='
    echo 'message=Admin deployment is ready.'
    printf 'updatedAt=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  } > "$project_dir/runtime/deployment/deploy.status"
  chmod 0660 "$project_dir/runtime/deployment/deploy.status"
fi

sudo install -m 0644 "$script_dir/systemd/isahaedream-deploy.service" /etc/systemd/system/isahaedream-deploy.service
sudo install -m 0644 "$script_dir/systemd/isahaedream-deploy.path" /etc/systemd/system/isahaedream-deploy.path
sudo systemctl daemon-reload
sudo systemctl enable --now isahaedream-deploy.path
sudo systemctl status --no-pager isahaedream-deploy.path
