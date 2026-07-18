#!/usr/bin/env bash
# ── Личный VLESS + XHTTP узел за Caddy на gsgit.org ──────────────────────────
# Запускать из папки server/:  bash xray-setup.sh
#
# Генерит UUID + секретный путь, собирает конфиг Xray и сниппет Caddy, поднимает
# контейнер xray и перечитывает Caddy. Безопасно: путь Xray подключается через
# import — если снести сгенерённое, сайт продолжит работать как обычно.
# Идемпотентно: повторный запуск переиспользует уже сгенерённые секреты.
set -euo pipefail

DOMAIN="gsgit.org"
HERE="$(cd "$(dirname "$0")" && pwd)"
XRAY_DIR="$HERE/xray"
CONF_D="$HERE/caddy/conf.d"
CFG="$XRAY_DIR/config.json"
SNIPPET="$CONF_D/xray.caddy"
SECRETS="$XRAY_DIR/secrets.env"

mkdir -p "$XRAY_DIR" "$CONF_D"

# 1. Секреты: переиспользуем, если уже есть.
if [ -f "$SECRETS" ]; then
  # shellcheck disable=SC1090
  . "$SECRETS"
  echo "[i] Использую уже сгенерённые секреты ($SECRETS)."
else
  UUID="$(cat /proc/sys/kernel/random/uuid)"
  PATH_SECRET="/$(head -c 8 /dev/urandom | od -An -tx1 | tr -d ' \n')"
  printf 'UUID=%s\nPATH_SECRET=%s\n' "$UUID" "$PATH_SECRET" > "$SECRETS"
  chmod 600 "$SECRETS"
  echo "[i] Сгенерированы новые секреты."
fi

# 2. Конфиг Xray из шаблона.
sed -e "s|__UUID__|$UUID|" -e "s|__PATH__|$PATH_SECRET|" \
  "$XRAY_DIR/config.template.json" > "$CFG"

# 3. Сниппет Caddy: реверс-прокси секретного пути на xray внутри docker-сети.
#    flush_interval -1 отключает буферизацию (нужно XHTTP), h2c — plaintext HTTP/2.
cat > "$SNIPPET" <<EOF
@xray path ${PATH_SECRET}/*
handle @xray {
	reverse_proxy h2c://xray:2000 {
		flush_interval -1
	}
}
EOF

# 4. Проверяем конфиг Caddy ДО перезапуска — если что-то не так, не трогаем прод.
echo "[i] Проверяю конфиг Caddy…"
if ! docker compose exec -T caddy caddy validate --config /etc/caddy/Caddyfile >/dev/null 2>&1; then
  # Caddy ещё не запущен или недоступен — провалидируем разово временным контейнером.
  docker run --rm -v "$HERE/caddy/Caddyfile:/etc/caddy/Caddyfile:ro" \
    -v "$CONF_D:/etc/caddy/conf.d:ro" caddy:2-alpine \
    caddy validate --config /etc/caddy/Caddyfile
fi

# 5. Поднимаем xray и перечитываем Caddy (reload fail-safe: при ошибке останется старый).
echo "[i] Поднимаю xray и перечитываю Caddy…"
docker compose --profile xray up -d xray
docker compose exec -T caddy caddy reload --config /etc/caddy/Caddyfile || \
  docker compose restart caddy

# 6. Клиентская ссылка.
ENC_PATH="$(printf '%s' "$PATH_SECRET" | sed 's|/|%2F|g')"
LINK="vless://${UUID}@${DOMAIN}:443?encryption=none&security=tls&sni=${DOMAIN}&host=${DOMAIN}&type=xhttp&path=${ENC_PATH}&mode=auto#GsGit-VPN"

echo
echo "═══════════════════════════════════════════════════════════════"
echo " Готово. Личный XHTTP-узел поднят."
echo
echo "  Домен : ${DOMAIN}"
echo "  Порт  : 443 (TLS терминирует Caddy)"
echo "  UUID  : ${UUID}"
echo "  Path  : ${PATH_SECRET}"
echo
echo " Ссылка для v2rayNG / NekoBox / Hiddify (импорт из буфера):"
echo
echo "  ${LINK}"
echo
echo "═══════════════════════════════════════════════════════════════"
echo " Проверка:  curl -s -o /dev/null -w '%{http_code}\\n' https://${DOMAIN}/"
echo " (сайт должен отдавать 200 — узел его не сломал)"
echo " Откат:     rm ${SNIPPET} && docker compose exec caddy caddy reload --config /etc/caddy/Caddyfile"
echo "            docker compose --profile xray stop xray"
echo "═══════════════════════════════════════════════════════════════"
