# PLANS.md

Этот файл — рабочий план проекта.
Его задача: не дать длинным задачам превратиться в хаос.

## Правила использования

Когда задача:

- больше чем на 1–2 файла;
- затрагивает несколько слоев;
- требует принятия решения;
- или выполняется в несколько заходов,

сначала обнови или используй этот файл как execution plan.

## Статусы

- `todo`
- `in_progress`
- `blocked`
- `done`
- `deferred`

---

## Общая стратегия доставки

Приоритет не на максимальную автоматизацию, а на ранний рабочий цикл:

1. поймали событие;
2. сохранили локально;
3. показали на экране;
4. показали на виджете;
5. дали исправить ошибку;
6. потом уже улучшаем точность.

---

## Текущий статус репозитория

На текущем этапе уже сделано:

- [done] создан минимальный Android `app` module;
- [done] подключены Kotlin, Jetpack Compose и базовая навигация;
- [done] подключен Room как dependency без реализации persistence-слоя;
- [done] создан стартовый экран-заглушка;
- [done] заведена package structure под `app / collection / parsing / domain / data / widget / ui / common`;
- [done] реализованы доменные модели `RawNotificationEvent`, `PaymentCandidate`, `PaymentEvent`, `DailySpendSummary`;
- [done] реализован Room schema slice для `RawNotificationEvent`: entity, dao, database wiring, mapper и repository skeleton;
- [done] реализован `NotificationListenerService` skeleton с app-level wiring;
- [done] добавлены strict allowlist policy и mapper `StatusBarNotification -> RawNotificationEvent`;
- [done] зафиксированы первые реальные package names: `ru.nspk.mirpay` и `ru.alfabank.mobile.android`;
- [done] добавлен debug-screen сырых уведомлений;
- [done] добавлены parser contract, parser registry и первый Mir Pay parser с unit tests;
- [done] добавлен первый parser для Альфа-Банка и согласован first-bank allowlist;
- [done] добавлен первый parser для Сбера и расширен allowlist для `ru.sberbankmobile`;
- [done] после реальных device samples bank parsing временно отключен в runtime; Mir Pay оставлен единственным платежным parser, а bank packages сохранены только для raw capture;
- [done] добавлена cleanup migration `4 -> 5`, которая удаляет ранее накопленные standalone `BANK` false positives;
- [done] реализован normalization / first `PaymentEvent` slice: normalizer, explainable confidence, `payment_events` storage и `sourceIds`;
- [done] подключен end-to-end pipeline `raw event -> parser -> normalizer -> dedupe -> payment event save`;
- [done] реализован `Today summary use case` поверх сохраненных `PaymentEvent` с агрегацией `CONFIRMED` и `CORRECTED` событий за локальный текущий день;
- [done] `TodayScreen` подключен к реальному summary flow и показывает состояния `loading / empty / ready / no-permission`;
- [done] на экран `Today` добавлен read-only список сегодняшних `PaymentEvent`, вошедших в итог;
- [done] реализован onboarding / permission flow с отдельным стартовым route для notification access;
- [done] реализован первый manual correction slice: review-screen, confirm / correct amount / dismiss, `payment_event_edits` audit trail и реактивное обновление summary / widget;
- [done] реализован первый dedupe slice: online merge для пары `Mir Pay + bank` по одинаковой сумме и близкому времени, с merged `HYBRID` событием и `HIGH` confidence;
- [done] реализован duplicate conflict fallback: near-match `Mir Pay + bank` пары временно переводятся в `SUSPECTED`, исключаются из total, получают `duplicateGroupId` и разрешаются через существующий review flow;
- [done] реализован widget private mode: persisted global toggle на `Today`, regular/private widget states и count-only private summary без суммы;
- [done] реализован daily limit slice: глобальный лимит в `SharedPreferences`, отдельный `Settings` screen, `remainingAmountMinor` в `DailySpendSummary`, warning thresholds `80% / 100%` и limit-aware widget/private mode;
- [done] реализован onboarding / disclosure polish: общий permission/disclosure copy для onboarding и `Today`, privacy-first messaging и retry-friendly состояние после возврата из notification access settings без выданного доступа;
- [done] добавлены Gradle wrapper и version catalog;
- [done] настроен локальный `local.properties` для Android SDK;
- [done] проект собирается через `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
- [done] ingestion Mir Pay replay-дублей сделан idempotent по exact `source_package + payload_hash`, debug-screen ограничен Mir Pay, а migration `5 -> 6` чистит уже накопленные exact raw/payment duplicates.
- [done] debug-screen получил экспорт диагностического лога за сегодня: plain-text copy/share snapshot по raw notifications, payment events и manual edits без нового persistent processing journal.

Что еще не сделано:

- [done] первый widget slice;
- [done] manual correction flow;
- [done] дневной лимит и мягкие пороги предупреждения;
- [done] дальнейшие confidence improvements и duplicate fallback для сложных кейсов;
- [done] onboarding / disclosure polish.
- [done] parser coverage expansion slice для первого Sber template.
- [todo] собрать реальные bank payment samples и только потом возвращать bank parsing в runtime.

Следующий рекомендуемый шаг:

- [todo] перейти к raw sample collection для реальных bank payment notifications и только после этого безопасно реинтродуцировать bank parser.
- [todo] после накопления подтвержденных raw samples вернуть bank parsing точечно, только для реальных payment templates.

---

## Фазы проекта

## Phase 0 — Bootstrap

**Цель:** заложить чистую основу репозитория и Android-проекта.

### Задачи

- [done] Создать Android app module.
- [done] Подключить Kotlin, Compose, базовую структуру проекта.
- [done] Подключить Room.
- [done] Подготовить базовую навигацию / один стартовый экран.
- [done] Завести package structure под collection / parsing / domain / data / widget / ui.
- [done] Добавить базовые тестовые зависимости.

### Definition of done

- проект собирается;
- есть стартовый экран;
- структура пакетов соответствует архитектуре;
- нет случайных временных решений, которые потом придется ломать.

---

## Phase 1 — Raw notification capture

**Цель:** научиться надежно получать и сохранять сырые уведомления.

### Задачи

- [done] Реализовать `NotificationListenerService`.
- [done] Добавить onboarding для доступа к уведомлениям.
- [done] Сохранять поддержанные сырые уведомления в локальную БД.
- [done] Сделать debug-screen / developer-screen со списком raw events.
- [done] Добавить фильтрацию по `sourcePackage`.
- [done] Зафиксировать первые реальные package names в strict allowlist.

### Definition of done

- после выдачи разрешения приложение получает posted notifications;
- raw events сохраняются локально;
- разработчик может визуально проверить, что уведомления реально ловятся;
- событие не теряется между перезапусками процесса.

---

## Phase 2 — First parser

**Цель:** распознавать сумму и создавать первый `PaymentEvent`.

### Задачи

- [done] Определить `RawNotificationEvent` и `PaymentEvent`.
- [done] Сделать parser interface.
- [done] Реализовать первые правила для Mir Pay уведомлений.
- [done] Реализовать первые правила для одного банковского уведомления.
- [done] Нормализовать сумму и время.
- [done] Ввести начальный explainable `confidence`.

### Definition of done

- минимум один Mir Pay шаблон и один банковский шаблон распознаются;
- сумма попадает в `PaymentEvent`;
- у события есть `confidence` и `sourceKind`;
- есть тесты на parser и normalization.

---

## Phase 3 — Today screen

**Цель:** показать дневной итог в приложении.

### Задачи

- [done] Реализовать `Today summary use case`.
- [done] Реализовать экран `Today`.
- [done] Показать total amount, count, last payment.
- [done] Показать список событий за день.
- [done] Показать state для пустого дня.
- [done] Показать state для отсутствия доступа к уведомлениям.

### Definition of done

- пользователь видит итог за день;
- можно проверить, какие события вошли в итог;
- UI не зависит напрямую от сырых уведомлений.

---

## Phase 4 — First widget

**Цель:** вынести результат на домашний экран.

### Задачи

- [done] Подготовить Glance widget.
- [done] Реализовать ready-state.
- [done] Реализовать no-permission state.
- [done] Реализовать no-data state.
- [done] Подключить обновление виджета при изменении данных.

### Definition of done

- виджет можно добавить на домашний экран;
- он показывает дневной итог;
- он корректно реагирует на пустые данные и отсутствие permission.

---

## Phase 5 — Manual correction

**Цель:** закрыть честный fallback, без которого проект будет хрупким.

### Задачи

- [done] Экран спорных событий.
- [done] Подтверждение события.
- [done] Исправление суммы.
- [done] Отклонение ложного события.
- [done] Пометка `userEdited`.
- [done] Audit trail для ручных правок в `payment_event_edits`.

### Definition of done

- пользователь может исправить ошибку распознавания;
- итог дня корректно пересчитывается;
- система хранит, что событие было исправлено вручную.

---

## Phase 6 — Dedupe and confidence improvements

**Цель:** объединять Mir Pay и банк в одно событие.

### Задачи

- [done] Реализовать matching rules по времени и сумме для простого кейса `Mir Pay + bank`.
- [done] Реализовать первый online dedupe.
- [done] Повышать confidence до `HIGH` при успешном подтвержденном merge.
- [done] Расширить confidence rules и конфликтные кейсы без агрессивного auto-merge.
- [done] Добавить тесты на конфликтные кейсы.

### Definition of done

- два уведомления об одной оплате не раздувают дневной итог;
- система объяснимо объединяет события;
- спорные случаи не скрываются.

---

## Phase 7 — Privacy and UX polish

**Цель:** сделать продукт безопасным и приятным в реальном использовании.

### Задачи

- [done] Реализовать private mode виджета.
- [done] Добавить настройку лимита на день.
- [done] Добавить мягкие пороги предупреждения.
- [done] Улучшить тексты onboarding и disclosure.

### Definition of done

- виджет не пересвечивает чувствительные данные без необходимости;
- пользователь понимает, зачем дал доступ;
- приложение не выглядит как серый технический прототип.

---

## Текущий рекомендуемый порядок ближайших маленьких задач

1. Parser coverage expansion.
2. Explainable confidence tuning for newly supported bank templates.
3. Additional bank-specific parser tests from real notification samples.

---

## Task template для Codex

Для любой нетривиальной задачи используй такой шаблон:

### Task
Коротко: что делаем.

### Why
Зачем это нужно для core use case.

### Scope
Что входит.

### Out of scope
Что специально не делаем сейчас.

### Files likely touched
Список ожидаемых файлов.

### Risks
Что можно сломать.

### Verification
Какие команды, тесты или ручные проверки подтвердят результат.

### Docs to update
Какие документы надо обновить после изменения.

---

## Шаблон для небольшого вертикального шага

Пример хорошей постановки задачи для Codex:

> Сначала предложи короткий план. Потом реализуй только локальный слой хранения для `RawNotificationEvent` без UI. Не трогай widget, parser и onboarding. После изменений перечисли файлы и способ проверки.

---

## Правило по ретроспективам

Если после завершения задачи стало ясно, что:

- фазу надо перестроить;
- порядок работ был плохим;
- архитектура была описана расплывчато;
- Codex системно ошибается,

обнови этот файл.
