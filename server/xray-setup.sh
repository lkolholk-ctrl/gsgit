#!/usr/bin/env bash
# ── Личный VLESS + XHTTP узел на выделенном поддомене за Caddy ───────────────
# Запускать из папки server/:  bash xray-setup.sh
#
# Схема как на проверенном сервере: отдельный поддомен cdn.gsgit.org целиком
# уходит в Xray (path /), сайт gsgit.org не трогается вообще. Caddy сам выпускает
# TLS-сертификат для поддомена. Требуется DNS A-запись cdn.gsgit.org → IP сервера.
#
# Безопасно и обратимо: без сгенерённого сниппета поддомена не существует,
# gsgit.org/api/status работают как обычно. Идемпотентно (переиспользует секреты).
set -euo pipefail

SUBDOMAIN="${1:-cdn.gsgit.org}"
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
  echo "[i] Использую уже сгенерённый UUID ($SECRETS)."
else
  UUID="$(cat /proc/sys/kernel/random/uuid)"
  printf 'UUID=%s\n' "$UUID" > "$SECRETS"
  chmod 600 "$SECRETS"
  echo "[i] Сгенерирован новый UUID."
fi

# 2. Конфиг Xray из шаблона (path / — весь поддомен наш).
sed -e "s|__UUID__|$UUID|" "$XRAY_DIR/config.template.json" > "$CFG"

# 3. Сниппет Caddy: целый поддомен → xray внутри docker-сети.
#    flush_interval -1 отключает буферизацию (нужно XHTTP), h2c — plaintext HTTP/2.
cat > "$SNIPPET" <<EOF
${SUBDOMAIN} {
	reverse_proxy h2c://xray:2000 {
		flush_interval -1
	}
}
EOF

# 4. Валидируем конфиг Caddy ДО перезапуска.
echo "[i] Проверяю конфиг Caddy…"
docker run --rm -v "$HERE/caddy/Caddyfile:/etc/caddy/Caddyfile:ro" \
  -v "$CONF_D:/etc/caddy/conf.d:ro" caddy:2-alpine \
  caddy validate --config /etc/caddy/Caddyfile

# 5. Поднимаем xray и перечитываем Caddy (reload fail-safe).
echo "[i] Поднимаю xray и перечитываю Caddy…"
docker compose --profile xray up -d xray
docker compose exec -T caddy caddy reload --config /etc/caddy/Caddyfile || \
  docker compose restart caddy

# 6. Ждём и проверяем, что xray реально поднялся.
sleep 2
if [ "$(docker inspect -f '{{.State.Running}}' "$(docker compose ps -q xray)" 2>/dev/null)" != "true" ]; then
  echo
  echo "[!] Контейнер xray не запустился. Логи:"
  docker compose logs --tail 20 xray
  exit 1
fi

# 7. Клиентская ссылка (path /, extra-параметры зашиты в клиенте отдельно).
LINK="vless://${UUID}@${SUBDOMAIN}:443?encryption=none&security=tls&sni=${SUBDOMAIN}&host=${SUBDOMAIN}&type=xhttp&path=%2F&mode=auto&fp=chrome&alpn=h2%2Chttp%2F1.1#GsGit-VPN"

echo
echo "═══════════════════════════════════════════════════════════════"
echo " Готово. Личный XHTTP-узел поднят на выделенном поддомене."
echo
echo "  DNS   : добавь A-запись ${SUBDOMAIN} → IP этого сервера (если ещё нет)"
echo "  Адрес : ${SUBDOMAIN}"
echo "  Порт  : 443 (TLS выпускает Caddy автоматически)"
echo "  UUID  : ${UUID}"
echo "  Path  : /"
echo "  Extra : scMaxEachPostBytes=1000000, xPaddingBytes=100-1100 (уже в конфиге)"
echo
echo " Ссылка для v2rayNG / NekoBox / Hiddify (импорт из буфера):"
echo
echo "  ${LINK}"
echo
echo "═══════════════════════════════════════════════════════════════"
echo " Проверки:"
echo "   curl -I https://gsgit.org/                 # сайт жив (200)"
echo "   docker compose logs --tail 30 xray         # узел без ошибок"
echo "   curl -I https://${SUBDOMAIN}/              # 404/400 = Caddy проксирует в Xray (норма)"
echo
echo " Откат:  rm ${SNIPPET} && docker compose exec caddy caddy reload --config /etc/caddy/Caddyfile"
echo "         docker compose --profile xray stop xray"
echo "═══════════════════════════════════════════════════════════════"
