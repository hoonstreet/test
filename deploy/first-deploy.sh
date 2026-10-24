#!/usr/bin/env bash
set -euo pipefail

EXPECTED_PUBLIC_IP="15.164.87.34"
BLOCKED_PRODUCTION_IP="3.38.19.90"

for command_name in curl docker nginx openssl sudo systemctl; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Required command not found: $command_name" >&2
    exit 1
  fi
done

current_public_ip="$(curl --noproxy '*' -fsS https://checkip.amazonaws.com | tr -d '[:space:]')"

if [[ "$current_public_ip" == "$BLOCKED_PRODUCTION_IP" ]]; then
  echo "REFUSED: this script cannot run on the existing production server." >&2
  exit 1
fi

if [[ "$current_public_ip" != "$EXPECTED_PUBLIC_IP" ]]; then
  echo "REFUSED: expected IP $EXPECTED_PUBLIC_IP, current IP $current_public_ip" >&2
  exit 1
fi

echo "New server verified: $current_public_ip"

if [[ ! -f .env ]]; then
  read -r -s -p "Enter a NEW admin password (12+ characters): " admin_password
  echo
  read -r -s -p "Enter the SAME admin password again: " admin_password_confirm
  echo

  if [[ "$admin_password" != "$admin_password_confirm" ]]; then
    echo "Passwords do not match. Run the script again." >&2
    exit 1
  fi

  if (( ${#admin_password} < 12 )); then
    echo "Password must be at least 12 characters. Run the script again." >&2
    exit 1
  fi

  if [[ ! "$admin_password" =~ ^[A-Za-z0-9._@%+=:-]+$ ]]; then
    echo "Use only letters, numbers, and these symbols: . _ @ % + = : -" >&2
    exit 1
  fi

  postgres_password="$(openssl rand -hex 32)"
  umask 077
  {
    printf 'POSTGRES_PASSWORD=%s\n' "$postgres_password"
    printf 'ADMIN_USERNAME=admin\n'
    printf 'ADMIN_PASSWORD=%s\n' "$admin_password"
    printf 'BASE_URL=http://%s\n' "$EXPECTED_PUBLIC_IP"
    printf 'SEARCH_INDEX_ENABLED=false\n'
  } > .env
  chmod 600 .env
  unset admin_password admin_password_confirm postgres_password
  echo "Server-only settings created. Passwords will not be displayed."
else
  echo "Existing .env preserved."
fi

docker compose up -d --build

sudo install -m 0644 deploy/nginx/isahaedream.conf /etc/nginx/sites-available/isahaedream.conf
sudo ln -sfn /etc/nginx/sites-available/isahaedream.conf /etc/nginx/sites-enabled/isahaedream.conf

if [[ -e /etc/nginx/sites-enabled/default || -L /etc/nginx/sites-enabled/default ]]; then
  sudo mv /etc/nginx/sites-enabled/default /etc/nginx/sites-available/default.disabled-before-isahaedream
fi

sudo nginx -t
sudo systemctl reload nginx

docker compose ps
curl -fsS -o /dev/null -w 'local_http_status=%{http_code}\n' http://127.0.0.1:8080/
curl -fsS -o /dev/null -w 'public_http_status=%{http_code}\n' "http://${EXPECTED_PUBLIC_IP}/"

initial_backup="$(bash deploy/backup-db.sh)"
echo "Initial database backup: $initial_backup"

echo "Deployment complete: http://${EXPECTED_PUBLIC_IP}"
echo "Admin login: http://${EXPECTED_PUBLIC_IP}/admin/login"
