# GsGit server — моментальные пуши через api.gsgit.org

Стек: Caddy (авто-TLS) + Node-бэкенд без зависимостей (вебхуки GitHub → FCM) + Uptime Kuma (статус-страница).

## Схема

```
GitHub (вебхуки GsGit App) ──POST──▶ api.gsgit.org/webhook ──▶ FCM ──▶ телефон
Приложение ──POST /register (App user token + FCM token)──▶ api.gsgit.org
status.gsgit.org ──▶ Uptime Kuma
```

## Подготовка (один раз)

1. **DNS** (в панели регистратора): A-записи `@`, `api`, `status` → IP сервера.
2. **Firebase**: [console.firebase.google.com](https://console.firebase.google.com) →
   создать проект `gsgit` → Project settings → Service accounts →
   **Generate new private key** → скачанный JSON положить в `server/data/service-account.json`.
   Project ID оттуда же — в `.env`.
3. **Секрет вебхука**: `openssl rand -hex 32` → в `.env` и в настройки GsGit App
   (github.com/settings/apps → GsGit App → Webhook: URL `https://api.gsgit.org/webhook`,
   secret, Active ✓). В Permissions & events подписаться на события:
   Push, Issues, Issue comment, Pull request, Pull request review, Workflow run, Check suite, Release.

## Деплой на чистый Ubuntu 24.04

```bash
apt update && apt install -y docker.io docker-compose-v2 git
git clone https://github.com/lkolholk-ctrl/gsgit.git && cd gsgit/server
cp .env.example .env && nano .env          # заполнить секрет и project id
mkdir -p data                              # сюда же service-account.json
docker compose up -d --build
```

Проверка: `curl https://api.gsgit.org/healthz` → `{"ok":true,...}`.
Статус-страницу настроить в веб-интерфейсе `https://status.gsgit.org`
(первый вход — создание админа; добавить мониторы на api и github.com).

## Эндпоинты API

| Метод | Путь | Описание |
|---|---|---|
| GET | `/healthz` | живость + число зарегистрированных логинов |
| POST | `/webhook` | приёмник GitHub (HMAC `X-Hub-Signature-256`) |
| POST | `/register` | `Authorization: Bearer <App user token>`, body `{"fcmToken":"..."}` |
| POST | `/unregister` | body `{"fcmToken":"..."}` |

Логика адресатов: пуш получает владелец репозитория и назначенные/авторы
issue/PR из payload, кроме того, кто сам совершил действие (`sender`).

## Что дальше в приложении

Клиентская часть (после подключения Firebase в Android-проект):
`FirebaseMessagingService` принимает data-сообщение и строит нотификацию
через существующий notifications-слой; при логине приложение шлёт
`/register` с App user token и FCM-токеном. `NotificationSyncWorker`
остаётся как fallback-поллинг.
