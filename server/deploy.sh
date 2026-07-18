#!/usr/bin/env bash
# Автодеплой GsGit-сервера на чистый Ubuntu 24.04.
# Запуск (важно именно так, чтобы работали вопросы-ответы):
#   bash <(curl -fsSL https://raw.githubusercontent.com/lkolholk-ctrl/gsgit/claude/project-overview-d8kcpw/server/deploy.sh)
# Повторный запуск безопасен: уже сделанные шаги пропускаются.

set -euo pipefail

REPO_URL="https://github.com/lkolholk-ctrl/gsgit.git"
BRANCH="claude/project-overview-d8kcpw"
DIR="/root/gsgit"

say()  { printf '\n\033[1;32m==> %s\033[0m\n' "$*"; }
warn() { printf '\033[1;33m[!] %s\033[0m\n' "$*"; }

[ "$(id -u)" = 0 ] || { warn "Запусти от root"; exit 1; }

# ── 1. Swap 1 ГБ ─────────────────────────────────────────────────────────────
if ! swapon --show | grep -q /swapfile; then
  say "Создаю swap 1 ГБ"
  fallocate -l 1G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
else
  say "Swap уже есть — пропускаю"
fi

# ── 2. Docker + git ──────────────────────────────────────────────────────────
if ! command -v docker >/dev/null; then
  say "Ставлю Docker и git"
  apt-get update -q
  apt-get install -y -q docker.io docker-compose-v2 git curl openssl
else
  say "Docker уже установлен — пропускаю"
fi

# ── 3. Код ───────────────────────────────────────────────────────────────────
if [ -d "$DIR/.git" ]; then
  say "Обновляю репозиторий"
  git -C "$DIR" pull --ff-only
else
  say "Клонирую репозиторий (ветка $BRANCH)"
  git clone -b "$BRANCH" "$REPO_URL" "$DIR"
fi
cd "$DIR/server"
mkdir -p data

# ── 4. Секреты (.env) ────────────────────────────────────────────────────────
if [ ! -f .env ]; then
  say "Настраиваю .env"
  SECRET=$(openssl rand -hex 32)
  echo
  echo "────────────────────────────────────────────────────────────"
  echo "  СЕКРЕТ ВЕБХУКА (скопируй сейчас — он нужен в настройках"
  echo "  GsGit App на GitHub, поле Webhook secret):"
  echo
  echo "  $SECRET"
  echo "────────────────────────────────────────────────────────────"
  echo
  read -r -p "Firebase Project ID (можно оставить пустым и вписать позже): " FCM_ID || FCM_ID=""
  cat > .env <<EOF
GITHUB_WEBHOOK_SECRET=$SECRET
FCM_PROJECT_ID=$FCM_ID
SERVICE_ACCOUNT_FILE=/data/service-account.json
EOF
else
  say ".env уже есть — пропускаю (секрет смотри в $DIR/server/.env)"
fi

# ── 5. Ключ Firebase ─────────────────────────────────────────────────────────
if [ ! -s data/service-account.json ]; then
  echo
  read -r -p "Вставить ключ Firebase (service-account.json) сейчас? [y/N]: " PASTE || PASTE=""
  if [ "${PASTE,,}" = "y" ]; then
    echo "Вставь содержимое JSON целиком и нажми Enter, затем Ctrl+D:"
    cat > data/service-account.json || true
    if [ -s data/service-account.json ]; then
      say "Ключ сохранён"
    else
      rm -f data/service-account.json
      warn "Пусто — пропускаю. Позже: nano $DIR/server/data/service-account.json"
    fi
  else
    warn "Ок, без ключа пуши не поедут. Позже: nano $DIR/server/data/service-account.json"
  fi
fi

# ── 6. Запуск ────────────────────────────────────────────────────────────────
say "Собираю и запускаю контейнеры"
docker compose up -d --build

say "Жду api…"
for i in $(seq 1 20); do
  sleep 3
  if curl -fsS http://localhost:8080/healthz >/dev/null 2>&1 \
     || docker compose exec -T api node -e "fetch('http://localhost:8080/healthz').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))" >/dev/null 2>&1; then
    break
  fi
done

echo
say "Готово. Проверка снаружи (нужны DNS-записи на этот сервер):"
echo "    curl https://api.gsgit.org/healthz"
echo
echo "Дальше по чек-листу:"
echo "  1. DNS: A-записи @, api, status -> IP этого сервера (если ещё нет)"
echo "  2. GsGit App на GitHub -> Webhook: URL https://api.gsgit.org/webhook,"
echo "     secret — строка, показанная выше, события: Push, Issues, Issue comment,"
echo "     Pull request, Pull request review, Workflow run, Check suite, Release"
echo "  3. Статус-страница: открой https://status.gsgit.org и создай админа"
echo "  4. Ключ Firebase, если пропустил: nano $DIR/server/data/service-account.json"
echo "     и затем: cd $DIR/server && docker compose restart api"
