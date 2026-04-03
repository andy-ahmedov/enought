# PROMPT_RECIPES.md

Ниже готовые промпты для постепенной работы над проектом.
Они специально сделаны узкими.

Текущее состояние репозитория:

- bootstrap Android-проекта уже выполнен;
- доменные модели уже реализованы;
- raw-event storage на Room уже реализован;
- `NotificationListenerService` skeleton уже реализован;
- strict allowlist для raw capture из Mir Pay, Альфа-Банка и Сбера уже реализован;
- mapper `StatusBarNotification -> RawNotificationEvent` уже реализован;
- debug-screen сырых уведомлений уже реализован;
- parser contract и активный Mir Pay parser уже реализованы;
- bank raw capture сохранен, но bank parsing в runtime временно выключен после реальных device samples с ложными срабатываниями;
- first normalization / `PaymentEvent` slice уже реализован;
- `payment_events` и `payment_event_sources` storage уже реализованы;
- end-to-end pipeline из listener в normalized event уже реализован;
- `Today summary use case` уже реализован;
- первый `Today` screen slice с состояниями `loading / empty / ready / no-permission` уже реализован;
- список сегодняшних `PaymentEvent` под summary на экране `Today` уже реализован;
- onboarding / permission flow с отдельным стартовым route уже реализован;
- первый widget slice уже реализован;
- минимальный manual correction flow уже реализован;
- persisted widget private mode уже реализован;
- daily limit slice уже реализован;
- duplicate conflict fallback уже реализован;
- onboarding / disclosure polish уже реализован;
- проект собирается через `./gradlew :app:testDebugUnitTest :app:assembleDebug`;
- следующий рекомендуемый prompt для новой сессии — `24. Real bank sample collection`.

---

## 1. Bootstrap Android-проекта

> Сначала прочитай `AGENTS.md`, `README.md` и `PLANS.md`. Потом предложи короткий план. После этого подготовь минимальный Android app module с Kotlin + Compose без лишних фич. Не реализуй parser, widget и NotificationListenerService. В конце перечисли измененные файлы и как собрать проект.

---

## 2. Domain model

> Прочитай `AGENTS.md` и `docs/ARCHITECTURE.md`. Реализуй только доменные модели `RawNotificationEvent`, `PaymentCandidate`, `PaymentEvent`, `DailySpendSummary`. Пока без Room, без UI и без parser. Сделай минимально чистую структуру пакетов. В конце перечисли измененные файлы.

---

## 3. Room schema for raw events

> Прочитай `AGENTS.md`, `docs/ARCHITECTURE.md` и `PLANS.md`. Реализуй только слой локального хранения для `RawNotificationEvent`: entity, dao, database wiring, repository skeleton. Не трогай parser, widget и UI. В конце покажи, какие файлы изменены и как можно проверить, что схема корректна.

---

## 4. Notification listener skeleton

> Прочитай `AGENTS.md`, `docs/ARCHITECTURE.md` и ADR-0001. Сначала предложи короткий план. Потом реализуй `NotificationListenerService`, который пока только собирает и сохраняет сырые уведомления. Не пытайся распознавать суммы и не делай business logic в сервисе. Нужен только сбор raw events и минимальный путь проверки.

---

## 5. Debug screen for raw notifications

> Прочитай `AGENTS.md` и `docs/ARCHITECTURE.md`. Реализуй только debug-screen со списком сохраненных raw notifications. Не делай итог за день, widget и parser. Это должен быть инструмент проверки ingestion pipeline.

---

## 6. First parser for Mir Pay

> Прочитай `AGENTS.md`, `docs/ARCHITECTURE.md` и skill `notification-pipeline`. Реализуй только первый parser для уведомлений Mir Pay: `canParse`, `parse`, unit tests. Не делай dedupe и не трогай widget.

---

## 7. First parser for one bank

> Прочитай `AGENTS.md`, `docs/ARCHITECTURE.md` и `PLANS.md`. Реализуй поддержку одного банковского уведомления как отдельное parser rule. Не делай универсальный mega-parser. В конце покажи, как это встраивается в pipeline.

---

## 8. Today summary use case

> Прочитай `AGENTS.md` и `docs/ARCHITECTURE.md`. Реализуй только use case и repository flow для `DailySpendSummary` за сегодня. Не делай экран и виджет. Нужен только domain/data слой и тесты, если уместно.

---

## 9. Today screen

> Прочитай `AGENTS.md`, `docs/ARCHITECTURE.md` и ADR-0002. Реализуй только экран `Today`, который показывает summary за день. Не добавляй settings, manual correction и widget. Состояния: loading, empty, ready.

---

## 10. Today event list

> Прочитай `AGENTS.md`, `docs/ARCHITECTURE.md` и `PLANS.md`. Реализуй только следующий slice для `Today`: покажи список сегодняшних `PaymentEvent` под уже готовым summary. Не трогай widget, manual correction и onboarding. Источник истины только `PaymentEvent` / use case, без прямого чтения raw notifications.

---

## 11. Onboarding / permission flow

> Прочитай `AGENTS.md`, `docs/ARCHITECTURE.md`, ADR-0002 и `PLANS.md`. Реализуй только onboarding / permission slice для notification access: объяснение, зачем приложению нужен доступ к уведомлениям, переход в системные настройки и корректный возврат в `Today`. Не трогай widget, manual correction и dedupe.

---

## 12. First widget

> Прочитай `AGENTS.md`, `docs/ARCHITECTURE.md` и skill `widget-privacy`. Реализуй только первый Glance widget с состояниями `NoData` и `Ready`. Не трогай manual correction и privacy mode, если это усложняет первый шаг.

---

## 13. Privacy mode for widget

> Прочитай `AGENTS.md`, ADR-0002 и skill `widget-privacy`. Добавь private mode для виджета так, чтобы по умолчанию можно было скрывать точную сумму. Не меняй parser и storage.

---

## 14. Manual correction flow

> Прочитай `AGENTS.md`, `docs/ARCHITECTURE.md` и `PLANS.md`. Реализуй минимальный review-flow для `low confidence` события: подтверждение, исправление суммы и отклонение. Не усложняй UX и не добавляй лишние настройки.

---

## 15. Dedupe

> Прочитай `AGENTS.md`, ADR-0001 и `docs/ARCHITECTURE.md`. Добавь минимально объяснимый dedupe между Mir Pay и банком по времени и сумме. Сначала кратко опиши правила. Не делай сложную эвристику, если хватает простого и проверяемого решения.

---

## 21. Daily limit

> Прочитай `AGENTS.md`, ADR-0002, `docs/ARCHITECTURE.md` и `PLANS.md`. Реализуй следующий behavioral slice: локальный дневной лимит, remaining amount в `DailySpendSummary`, и спокойное отображение прогресса на экране `Today` и в widget. Не делай settings-overkill, cloud sync и сложную аналитику.

---

## 22. Confidence improvements

> Прочитай `AGENTS.md`, ADR-0001, `docs/ARCHITECTURE.md`, `PLANS.md` и skill `notification-pipeline`. Реализуй следующий explainable slice после daily limit: дальнейшие confidence improvements и duplicate fallback для конфликтных кейсов без агрессивного auto-merge и без тяжелого duplicate review UX.

---

## 23. Onboarding polish

> Прочитай `AGENTS.md`, ADR-0002, `docs/ARCHITECTURE.md` и `PLANS.md`. Реализуй следующий UX slice: улучшить onboarding / disclosure тексты и переходы вокруг notification access, не меняя ingestion pipeline, dedupe, review flow и widget contracts.

---

## 24. Real bank sample collection

> Прочитай `AGENTS.md`, ADR-0001, `docs/ARCHITECTURE.md`, `PLANS.md` и skill `notification-pipeline`. Не меняй runtime parsing. Усиль raw-debug loop для сбора реальных bank payment notifications: помоги отличать настоящие payment samples от промо-уведомлений и подготовь безопасную базу для будущего возвращения bank parser rules.

---

## 16. Review already-made diff

> Не меняй код. Проведи review текущего diff с фокусом на: 1) архитектурный дрейф, 2) privacy, 3) edge cases, 4) test gaps. Сначала прочитай `AGENTS.md` и релевантные ADR.

---

## 17. Multi-agent architecture review

> Spawn one subagent to inspect architecture drift, one to inspect privacy / permission issues, and one to inspect missing tests. Wait for all of them, then summarize the findings. Do not modify files.

---

## 18. Update docs after decision

> Прочитай `AGENTS.md`, `README.md`, `docs/ARCHITECTURE.md` и `PLANS.md`. На основе текущих изменений обнови только документацию: что поменялось, что стало новым источником истины, и какие следующие шаги логичны. Код не меняй.

---

## 19. Stop drift prompt

> Остановись и вернись к `AGENTS.md` и relevant ADR. Ты начал выходить за рамки архитектуры проекта. Предложи новый короткий план строго в рамках `NotificationListener + manual correction + local-first + private widget`.

---

## 20. First normalization / PaymentEvent slice

> Прочитай `AGENTS.md`, `docs/ARCHITECTURE.md`, `PLANS.md` и skill `notification-pipeline`. Реализуй только следующий шаг после первых parser-ов: из `PaymentCandidate` сделать первый нормализованный `PaymentEvent` без dedupe, widget и Today UI. Нужны explainable confidence rules, mapping `sourceIds`, локальные unit tests и обновление roadmap/docs.
