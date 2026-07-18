# Промпт для генерации лендинга gsgit.org

> Скопируй всё, что ниже черты, в Kimi K3 одним сообщением.
> Скриншоты: подготовь 4–6 PNG (см. список в конце файла) и положи рядом с
> готовым index.html в папку `assets/` под именами `screen-1.png` … `screen-6.png`.
> Лендинг генерируется сразу с этими путями — просто подменишь файлы.

---

Сделай продуктовый лендинг для Android-приложения **GsGit** — опенсорсного
GitHub-клиента. Одна страница `index.html` + отдельная страница `privacy.html`.

## Жёсткие технические требования

- Каждая страница — **один самодостаточный HTML-файл**: весь CSS и JS инлайном.
  **Никаких** внешних CDN, файлов со сторонних доменов, npm, билд-систем,
  веб-шрифтов по ссылкам. Шрифт: стек `"JetBrains Mono", ui-monospace,
  "Cascadia Mono", monospace`.
- Mobile-first, полностью адаптивно (от 360px до десктопа). Без горизонтального
  скролла страницы.
- Быстро: без тяжёлых анимаций, без библиотек. Ванильный JS только там, где
  нужен (плавный скролл к якорям, лайтбокс галереи — опционально).
- SEO/соцсети: `<title>`, meta description, Open Graph теги (og:title,
  og:description). Язык страницы — **английский**.
- Изображения скриншотов подключай по относительным путям
  `assets/screen-1.png` … `assets/screen-6.png`. Пока файлов нет — покажи
  аккуратные плейсхолдеры с подписью, вёрстка не должна ломаться без картинок.

## Стиль

Тёмная терминальная эстетика, как в самом приложении: почти чёрный фон,
моноширинный шрифт, сдержанные неоново-зелёные акценты, тонкие рамки, лёгкий
glassmorphism (полупрозрачные карточки с блюром). Заголовки можно оформлять
в духе терминала: `> gsgit`, `$ ./download`. Скриншоты телефона — в тонких
CSS-рамках устройства (без картинок-мокапов). Никакой пестроты: это инструмент
для разработчиков, строгость = доверие.

Не используй логотип GitHub или октокота как логотип продукта — только текстовое
имя «GsGit» и терминальные глифы.

## Структура index.html

1. **Hero**: имя GsGit, слоган (придумай короткий, про «GitHub in your pocket,
   pushed to you in seconds» — можешь улучшить), две кнопки:
   - `Download APK` → https://github.com/lkolholk-ctrl/gsgit/releases/latest
   - `Source code` → https://github.com/lkolholk-ctrl/gsgit
   Подпись под кнопками: Android 8.0+, free & open source.
2. **Killer-фичи** (карточки, 3 главные):
   - **Sign in without tokens** — авторизация через GitHub App device flow:
     никакие PAT-токены не вводятся и не хранятся; короткоживущая сессия в
     шифрованном хранилище устройства.
   - **Instant push notifications** — собственный сервер уведомлений
     (api.gsgit.org): события GitHub прилетают на телефон за секунды через
     вебхуки, а не поллингом. 10 отдельных каналов уведомлений — от issues до
     стартов CI — каждый включается/выключается в системных настройках.
   - **Actions that feel native** — прогоны CI в реальном времени на экране,
     логи джобов, артефакты качаются в фоне с прогрессом прямо в системные
     Загрузки.
3. **Полный список возможностей** (компактная сетка): repos + file browser +
   code editor with syntax highlighting, issues/PRs/reviews, releases,
   discussions, gists, workflow dispatch, deep links for every github.com URL,
   commit diffs & compare, branch protection, webhooks management, security
   alerts on new device sign-in, dark terminal UI.
4. **Галерея**: 4–6 скриншотов (`assets/screen-*.png`) с короткими подписями.
5. **How instant pushes work** — маленькая схема-строка:
   `GitHub webhook → api.gsgit.org → FCM → your phone (~2 s)` + два предложения
   о том, что сервер хранит только GitHub-логин и push-токен устройства.
6. **Security & privacy** (кратко): open source, tokens never leave the device,
   encrypted storage, revoke access anytime from GitHub settings; ссылка на
   `privacy.html`.
7. **Footer**: GitHub · Releases · status.gsgit.org · privacy.html · © 2026 GsGit.

## Структура privacy.html

Тот же стиль, коротко и честно, по-английски:

- What we store on our server (api.gsgit.org): your GitHub login and the
  device push token — solely to deliver notifications. Nothing else; no
  analytics, no ads, no data sale.
- What stays on the device: GitHub App session tokens in encrypted storage.
- GitHub data is accessed through the official GitHub API on your behalf and
  is not persisted server-side.
- Data deletion: signing out (or disabling pushes) unregisters the device from
  the server; revoking the GsGit App on GitHub cuts all access.
- Contact: ссылка на GitHub Issues проекта.
- Дата последнего обновления.

---

## Какие скриншоты подготовить (для папки assets/)

| Файл | Что снять |
|---|---|
| screen-1.png | Главный экран со списком репозиториев |
| screen-2.png | Экран рана Actions (джобы/степы, лучше активный ран) |
| screen-3.png | Шторка с пуш-уведомлениями от GsGit (issue + CI) |
| screen-4.png | Редактор кода с подсветкой |
| screen-5.png | Pull request с ревью/чеками |
| screen-6.png | Системные настройки каналов уведомлений (10 тумблеров) |

Советы: тёмная тема, свежие аккуратные данные без личной почты/токенов,
одинаковая ориентация (портрет), родное разрешение телефона, PNG без сжатия.
