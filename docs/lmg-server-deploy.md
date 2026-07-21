# LMG-сервер — что делать НА СЕРВЕРЕ (пошагово)

Серверный код уже в репо `gsgit`, папка `server-lmg/`. Ставится на тот же VPS, что и GsGit-сервер.
Ниже — всё, что делаешь ты. Fable параллельно правит клиент, GPT — админ-вкладку.

---

## Шаг 1. Собрать ОДИН секрет (заранее)

**Партнёрский ключ ICM (ГЛАВНЫЙ и единственный обязательный):**
- Зайти на `https://byicloud.online/partners`, получить `X-Partner-Key`.
- Это тот самый ключ, что сейчас лежит в APK — переносим его на сервер.
- **Только на сервер, в чат/скрины не кидать.**

**Telegram-бот НЕ нужен.** ICM уже занёс наш `liquid.glassfiles.ru` в whitelist и origin, поэтому
партнёрка пускает по паре «партнёрский ключ + origin». Сервер сам шлёт `Origin: https://liquid.glassfiles.ru`
на upstream (env `ICM_ORIGIN`, дефолт уже верный). `TELEGRAM_BOT_TOKEN` оставляем пустым.

---

## Шаг 2. Подтянуть код на VPS

```bash
cd /root/gsgit
git fetch origin
git checkout claude/project-overview-d8kcpw   # если ещё не на ней
git pull origin claude/project-overview-d8kcpw
ls server-lmg/                                 # должны быть serverLMG.js, install.sh, .env.example ...
```

---

## Шаг 3. Заполнить .env

```bash
cd /root/gsgit/server-lmg
cp .env.example .env
nano .env
```
Вписать ОДНУ строку:
```
ICM_PARTNER_KEY=<ключ с byicloud.online/partners>
```
`TELEGRAM_BOT_TOKEN` — оставить пустым. Остальное (`LMG_PORT=8090`, `ICM_BASE_URL`,
`ICM_ORIGIN=https://liquid.glassfiles.ru`, `LMG_DATA_DIR=/data`) — по умолчанию. Ctrl+O, Enter, Ctrl+X.

---

## Шаг 4. Запустить

```bash
bash /root/gsgit/server-lmg/install.sh
```
Скрипт соберёт контейнер, поднимет, проверит health и покажет **admin-key** (запиши — он для админ-панели).
(Если предпочитаешь руками: `cd server-lmg && docker compose up -d --build`.)

---

## Шаг 5. Проверить, что живой

```bash
# admin-key взять из вывода install.sh или: docker compose exec -T lmg cat /data/lmg-admin.key
curl -s http://127.0.0.1:8090/admin/lmg/health -H "x-admin-key: <admin-key>" | jq
```
Ждём:
```json
{ "serverVersion":"1.0.1", "partnerKeySet":true, "icmOrigin":"https://liquid.glassfiles.ru", "icmUpstream":"ok", ... }
```
- `partnerKeySet:true` — ключ подхватился.
- `icmOrigin` — наш whitelisted origin, который сервер шлёт на ICM.
- `icmUpstream:"ok"` — сервер достучался до byicloud.
- `telegramHmac` — false (это норма, бот не нужен).

Проверка, что ключ реально рабочий (минтит сессию):
```bash
curl -s -X POST http://127.0.0.1:8090/admin/lmg/session/test -H "x-admin-key: <admin-key>" | jq
# ждём: { "upstreamStatus":200, "gotToken":true }
```
Если `gotToken:false` или `upstreamStatus` не 200 — партнёрский ключ неверный/не активирован.

---

## Шаг 6. Навесить домен (HTTPS)

Сервер слушает `:8090` локально. Домен — `api.gsgit.org/lmg` (переиспользуем существующий домен GsGit,
свой сертификат не нужен). Прокси на VPS — **Caddy**.

В Caddyfile, в блоке `api.gsgit.org`, добавить ДВА `handle` ПЕРЕД общим reverse_proxy:
```caddy
api.gsgit.org {
    handle /lmg/* {
        reverse_proxy localhost:8090
    }
    handle /admin/lmg/* {
        reverse_proxy localhost:8090
    }
    handle {
        reverse_proxy localhost:<порт-старого-GsGit-бэкенда>
    }
}
```
- `handle`, НЕ `handle_path` — префикс `/lmg` не срезаем (роуты сервера сами начинаются с `/lmg`).
- Caddy сам ставит `X-Forwarded-For` → rate-limit по IP считает реальный адрес.
- Применить: `caddy reload` (или `systemctl reload caddy`).

Итоговый `https://api.gsgit.org/lmg/icm` — это база для Fable (BASE_URL).

Проверка снаружи (после reload):
```bash
curl -s https://api.gsgit.org/lmg/config; echo
curl -s https://api.gsgit.org/admin/lmg/health -H "x-admin-key: <admin-key>"; echo
```

---

## Шаг 7. Раздать по ролям

- **Fable:** база `https://api.gsgit.org/lmg/icm` (для `lmg-client-changes-for-fable.md`).
- **GPT (админка):** `https://api.gsgit.org/lmg` + **admin-key** (для вкладки «LMG» — инструкцию напишу отдельно).

---

## Обновления сервера потом

```bash
cd /root/gsgit && git pull origin claude/project-overview-d8kcpw
cd server-lmg && docker compose up -d --build
```

## Если что-то не так
- `docker compose logs --tail=50` (в `/root/gsgit/server-lmg`) — смотреть старт/варны.
- `[warn] ICM_PARTNER_KEY не задан` в логах → `.env` не подхватился (проверь путь/синтаксис, пересобери).
- `icmUpstream:"down"` → VPS не достаёт byicloud (сеть/файрвол) или ключ отклонён.
- Порт 8090 занят → поменяй `LMG_PORT` в `.env` и в `docker-compose.yml`.
