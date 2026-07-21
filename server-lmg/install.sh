#!/usr/bin/env bash
# Автоустановка LMG-сервера. Запуск на VPS:  bash server-lmg/install.sh
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  cp .env.example .env
  echo "→ Создан server-lmg/.env. Впиши ICM_PARTNER_KEY (Telegram-токен не нужен), потом запусти снова."
  exit 1
fi

# читаем .env без экспорта секретов в лог
ICM_KEY=$(grep -E '^ICM_PARTNER_KEY=' .env | cut -d= -f2- || true)
if [ -z "${ICM_KEY}" ]; then
  echo "→ ICM_PARTNER_KEY пуст в server-lmg/.env — заполни и запусти снова."
  exit 1
fi

echo "→ Сборка и запуск контейнера…"
docker compose up -d --build

echo "→ Ждём старта…"; sleep 4
KEY=$(docker compose exec -T lmg sh -c 'cat /data/lmg-admin.key 2>/dev/null' || true)
echo "→ Проверка health:"
curl -s "http://127.0.0.1:${LMG_PORT:-8090}/admin/lmg/health" -H "x-admin-key: ${KEY}" || true
echo
echo "──────────────────────────────────────────"
echo "admin key (для админ-панели): ${KEY}"
echo "Ждём в health: partnerKeySet:true, icmUpstream:\"ok\" (telegramHmac:false — это норма)"
