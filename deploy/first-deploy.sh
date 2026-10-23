#!/usr/bin/env bash
set -euo pipefail

EXPECTED_PUBLIC_IP="15.164.87.34"
BLOCKED_PRODUCTION_IP="3.38.19.90"

for command_name in curl docker openssl sudo; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "필수 명령을 찾을 수 없습니다: $command_name" >&2
    exit 1
  fi
done

current_public_ip="$(curl --noproxy '*' -fsS https://checkip.amazonaws.com | tr -d '[:space:]')"

if [[ "$current_public_ip" == "$BLOCKED_PRODUCTION_IP" ]]; then
  echo "기존 운영 서버에서는 이 스크립트를 실행할 수 없습니다." >&2
  exit 1
fi

if [[ "$current_public_ip" != "$EXPECTED_PUBLIC_IP" ]]; then
  echo "대상 서버 불일치: 예상 IP $EXPECTED_PUBLIC_IP, 현재 IP $current_public_ip" >&2
  exit 1
fi

echo "신규 서버 확인 완료: $current_public_ip"

if [[ ! -f .env ]]; then
  read -r -s -p "관리자 비밀번호(12자 이상): " admin_password
  echo
  read -r -s -p "관리자 비밀번호 확인: " admin_password_confirm
  echo

  if [[ "$admin_password" != "$admin_password_confirm" ]]; then
    echo "관리자 비밀번호가 일치하지 않습니다." >&2
    exit 1
  fi

  if (( ${#admin_password} < 12 )); then
    echo "관리자 비밀번호는 12자 이상이어야 합니다." >&2
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
  echo "서버 전용 환경설정을 생성했습니다. 비밀번호는 출력하지 않습니다."
else
  echo "기존 .env를 유지합니다."
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

echo "배포가 완료되었습니다: http://${EXPECTED_PUBLIC_IP}"
echo "관리자 로그인: http://${EXPECTED_PUBLIC_IP}/admin/login"
