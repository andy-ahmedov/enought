# ARCHITECTURE.md

Этот документ — технический источник истины для реализации проекта.
Если `README.md` отвечает на вопрос «что и зачем мы строим», то этот файл отвечает на вопрос «как это устроено технически».

## 1. Суть выбранной архитектуры

Архитектура проекта строится вокруг идеи:

**не лезть в платежный стек и чужое приложение, а собирать легальные пользовательски-разрешенные сигналы после оплаты.**

Из этого следует финальная схема:

```text
Оплата смартфоном
      ↓
Mir Pay и/или банк публикуют уведомление
      ↓
NotificationListenerService ловит событие
      ↓
RawNotificationEvent сохраняется локально
      ↓
Parser превращает raw notification в PaymentCandidate
      ↓
Normalizer приводит данные к единому формату
      ↓
Matcher / Dedupe объединяет Mir Pay + банк
      ↓
PaymentEvent сохраняется локально
      ↓
DailySpendSummary считается из PaymentEvent
      ↓
Today screen + Widget
      ↓
Если confidence низкий → ручное подтверждение / исправление
```

## 2. Архитектурные инварианты

Пока пользователь явно не решит иначе, считаются обязательными следующие инварианты:

1. **Основной вход в систему — уведомления.**
2. **Mir Pay — важный источник сигнала, но не единственный и не центр архитектуры.**
3. **Банк — второй источник сигнала и подстраховка.**
4. **Любая сложная логика начинается после сохранения raw events.**
5. **Виджет не работает напрямую с raw notifications.**
6. **Спорные события должны иметь путь ручного подтверждения или исправления.**
7. **V1 работает локально, без обязательного сервера.**
8. **Приватность и скрытие чувствительных данных поддерживаются с первого виджета.**

## 3. Предлагаемая модульная разбивка внутри app

Это не жесткая структура папок, а ориентир по слоям.

```text
app/
  src/main/java/.../
    app/
      App.kt
      navigation/
    collection/
      notifications/
    parsing/
      rules/
      mappers/
    domain/
      model/
      usecase/
      repository/
    data/
      db/
      dao/
      entity/
      repository/
    widget/
    ui/
      onboarding/
      today/
      review/
      settings/
    common/
```

### Рекомендация

Не смешивать эти обязанности:

- `collection` не должен заниматься UI;
- `parser` не должен обновлять widget напрямую;
- `widget` не должен знать о сырых notification extras;
- `ui` не должен содержать regex-логику парсинга банковских текстов.

## 4. Слои подробно

## 4.1 Collection layer

### Ответственность

- подписаться на системный поток уведомлений через `NotificationListenerService`;
- отфильтровать интересующие пакеты;
- собрать максимум полезных полей из уведомления;
- сохранить raw event как можно ближе к исходному виду.

### На этом слое не делаем

- бизнес-решения о том, была ли это точно оплата смартфоном;
- дедупликацию;
- расчет дневного итога;
- логику виджета.

### Почему это важно

Если сразу смешать захват уведомления с интерпретацией, потом будет невозможно честно улучшать parser и отлаживать ложные срабатывания.

---

## 4.2 Parsing layer

### Ответственность

- взять `RawNotificationEvent`;
- определить, подходит ли он под известный шаблон;
- извлечь сумму, валюту, время, merchant, статус;
- вернуть промежуточный `PaymentCandidate`.

### Стратегия

Parser должен быть rule-based и расширяемым.
Не один giant parser на все случаи, а набор правил, например:

- `MirPayNotificationParser`
- `AlfaBankNotificationParser`
- `BankXNotificationParser`
- `BankYNotificationParser`

### Контракт parser-а

У каждого правила должно быть хотя бы:

- `canParse(rawEvent): Boolean`
- `parse(rawEvent): PaymentCandidate?`

Можно выбрать и другой API, но смысл должен остаться тем же.

Текущий первый реализованный вариант:

- `NotificationParser` как общий контракт;
- `NotificationParserRegistry` как ordered entrypoint;
- `MirPayNotificationParser` как первое узкое правило;
- `AlfaBankNotificationParser` как первое банковское правило;
- parser работает по уже сохраненным строковым полям `RawNotificationEvent`, без прямой зависимости от Android notification API.

---

## 4.3 Normalization layer

### Ответственность

Привести разные форматы к одной внутренней форме.

Что нормализуем:

- сумму в minor units;
- валюту;
- дату/время;
- тип источника;
- канал оплаты;
- merchant name;
- сырые признаки, влияющие на confidence.

### Почему отдельный слой

Потому что parser-ы банков будут разнородными. Нужно место, где разрозненные куски превращаются в единый формат домена.

На текущем этапе уже реализован первый normalization slice:

- из `PaymentCandidate` собирается одиночный `PaymentEvent`;
- при отсутствии dedupe `id` совпадает с `rawSourceId`;
- после успешного online dedupe канонический `PaymentEvent` сохраняет существующий `id`, а второй raw source добавляется в `sourceIds`;
- при конфликтном near-match fallback оба события получают общий `duplicateGroupId`, временно уходят в `SUSPECTED` и исключаются из total до ручного решения.

---

## 4.4 Matching / dedupe layer

### Ответственность

- понять, какие два сигнала относятся к одной оплате;
- схлопнуть дубли;
- улучшить уже выставленный начальный `confidence`;
- не раздувать итог за день.

### Базовая идея matching

Сначала простые и объяснимые правила:

- близкое время;
- совпадающая сумма;
- совместимые источники (`mir_pay` + `bank`);
- отсутствие признаков возврата/отмены;
- отсутствие уже связанного события.

На текущем первом dedupe slice уже реализовано:

- matching только online, в момент прихода нового `PaymentEvent`;
- автоматический merge только для пары `MIR_PAY + BANK`;
- окно матча: одинаковая сумма и валюта, разница по времени не больше 2 минут;
- конфликтный fallback для той же пары: near-match в окне до 5 минут или неоднозначный safe candidate не мержится автоматически, а переводит пару в `SUSPECTED`;
- автосхлопывание не применяется к `LOW`, `HYBRID`, `DISMISSED`, `CORRECTED`, `duplicateGroupId != null` и `userEdited` событиям;
- после успешного merge событие получает `sourceKind = HYBRID`, `confidence = HIGH`, `status = CONFIRMED` и объединенный список `sourceIds`.

### Не делать в начале

- сложный ML;
- черную коробку без объяснимости;
- агрессивное схлопывание, которое скрывает сомнения.

---

## 4.5 Persistence layer

### Ответственность

Локально хранить:

- raw notifications;
- нормализованные платежные события;
- пользовательские исправления;
- производные summary, если понадобится кеш.

### Базовый стек

- Room
- локальная БД

### Что хранить обязательно

#### Таблица `raw_notification_events`

Примерный состав:

- `id`
- `source_package`
- `posted_at`
- `title`
- `text`
- `sub_text`
- `big_text`
- `extras_json`
- `payload_hash`
- `created_at`

#### Таблица `payment_events`

Текущий минимальный состав:

- `id`
- `amount_minor`
- `currency`
- `paid_at`
- `merchant_name`
- `source_kind`
- `payment_channel`
- `confidence`
- `status`
- `user_edited`
- `duplicate_group_id`

#### Таблица `payment_event_sources`

Связь между нормализованным событием и raw source.
На текущем этапе одно событие может быть связано как с одним `rawSourceId`, так и с несколькими source ids после online dedupe.

#### Таблица `payment_event_edits`

История ручных правок.

---

## 4.6 Domain layer

### Ответственность

Инкапсулировать бизнес-логику приложения.

Пример use cases:

- `ObserveTodaySpendSummaryUseCase`
- `ConfirmPaymentEventUseCase`
- `CorrectPaymentAmountUseCase`
- `DismissPaymentEventUseCase`
- `RebuildDailySummaryUseCase`
- `ParseIncomingRawEventUseCase`

### Важное правило

Domain layer не должен знать об Android notification API напрямую.
Он должен работать с уже подготовленными внутренними моделями.

На текущем этапе уже реализован первый summary use case:

- `ObserveTodaySummaryUseCase` читает только `PaymentEvent`;
- summary считается для локального текущего дня через `Clock.zone`;
- в итог входят события со `status == CONFIRMED` и `status == CORRECTED`;
- конфликтные пары со `status == SUSPECTED` и общим `duplicateGroupId` честно исключаются из total до ручного решения;
- `hasLowConfidenceItems` теперь означает наличие нерешенных `SUSPECTED` событий в этом дне.

---

## 4.7 Widget layer

### Ответственность

- получить уже готовый summary;
- показать его в компактном виде;
- корректно отработать разные состояния;
- при изменении данных обновиться через app widget update flow.

### Минимальные состояния виджета

- `PermissionMissing`
- `NoData`
- `Ready`

На текущем первом widget slice уже реализовано:

- Glance-based homescreen widget;
- источник данных — только `ObserveTodaySummaryUseCase` + проверка notification access;
- `NoPermission`, `NoData`, `Ready` состояния;
- мягкий hint о suspected / low-confidence items внутри `NoData` и `Ready`, без отдельного review-state;
- обновление после сохранения нового `PaymentEvent` и при `MainActivity.onResume`, чтобы permission-state не залипал после возврата в приложение.

### Правила

- никакого парсинга уведомлений прямо в виджете;
- никакого прямого чтения raw event таблиц из composable без слоя подготовки;
- отдельная модель состояния виджета.

---

## 4.8 UI layer

### Экраны V1

- onboarding / permission setup;
- today screen;
- event review screen;
- settings / privacy mode / daily limit.

На текущем этапе первый `Today` screen slice уже реализован:

- onboarding / permission setup вынесен в отдельный стартовый route до `Today`, пока notification access не выдан;
- onboarding и `Today` no-permission state используют согласованный disclosure contract про supported notifications, local-first processing и private widget mode;
- onboarding различает первый вход и возврат из системных настроек без выданного доступа, чтобы показывать более точную retry-подсказку;
- экран читает только `ObserveTodaySummaryUseCase` и статус доступа к `NotificationListenerService`;
- raw notifications не используются напрямую в `Today` UI;
- поддержаны состояния `loading`, `no-permission`, `empty`, `ready`;
- в ready-state показываются total amount, count и last payment;
- под summary показывается read-only список сегодняшних `CONFIRMED` и `CORRECTED` `PaymentEvent`, которые вошли в итог;
- при наличии нерешенных `SUSPECTED` событий экран дает вход в review flow;
- debug-screen сырых уведомлений остается secondary developer action, а не основной UI-источник данных.

На текущем manual correction slice уже реализовано:

- отдельный review route для сегодняшних `SUSPECTED` событий;
- действия `confirm`, `correct amount`, `dismiss` для одиночных suspected items;
- pair-level действия `merge as one` и `keep both` для duplicate conflicts с общим `duplicateGroupId`;
- локальный audit trail в `payment_event_edits`;
- после ручного действия summary и widget обновляются через уже существующий `PaymentEvent`-поток.

### Экраны V1 не должны

- превращаться в универсальную банковскую ленту;
- показывать лишнюю детализацию, которая не помогает core use case.

## 5. Domain model draft

## 5.1 `RawNotificationEvent`

```kotlin
RawNotificationEvent(
  id,
  sourcePackage,
  postedAt,
  title,
  text,
  subText,
  bigText,
  extrasJson,
  payloadHash
)
```

## 5.2 `PaymentCandidate`

Промежуточная сущность между parser и final domain event.

```kotlin
PaymentCandidate(
  amountMinor?,
  currency?,
  paidAt?,
  merchantName?,
  sourceKind,
  paymentChannel,
  rawSourceId,
  confidenceHints
)
```

## 5.3 `PaymentEvent`

```kotlin
PaymentEvent(
  id,
  amountMinor,
  currency,
  paidAt,
  merchantName?,
  sourceKind,
  paymentChannel,
  confidence,
  status,
  userEdited,
  sourceIds,
  duplicateGroupId?
)
```

## 5.4 `DailySpendSummary`

```kotlin
DailySpendSummary(
  date,
  totalAmountMinor,
  paymentsCount,
  lastPaymentAmountMinor,
  limitAmountMinor,
  remainingAmountMinor,
  limitWarningLevel,
  hasLowConfidenceItems
)
```

Текущий summary slice использует такие правила:

- `totalAmountMinor`, `paymentsCount`, `lastPaymentAmountMinor` считаются по `CONFIRMED` и `CORRECTED` событиям;
- `limitAmountMinor` берется из локального preferences-repository и может быть `null`;
- `remainingAmountMinor = limitAmountMinor - totalAmountMinor` и может быть отрицательным;
- `limitWarningLevel`:
  - `null`, если лимит не задан или расход < 80% лимита;
  - `NEAR_LIMIT`, если расход >= 80% лимита и остаток еще положительный;
  - `LIMIT_REACHED`, если остаток `0` или меньше;
- `hasLowConfidenceItems` поднимается только если остались нерешенные `SUSPECTED` события;
- summary строится поверх `PaymentEvent`, а не raw notifications.

## 6. Confidence strategy

Confidence должен быть объяснимым и проверяемым.

### `HIGH`

- есть подтверждение из Mir Pay и банка;
- или очень сильный одиночный сигнал с однозначной суммой и временной привязкой.

### `MEDIUM`

- есть один хороший источник, но без второго подтверждения.

### `LOW`

- сумма распознана неуверенно;
- непонятно, это точно телефон или нет;
- есть признаки конфликта или неполноты.

### Следствие для UX

`LOW` не должен бесследно сливаться с обычным потоком.
Для него должен существовать review flow.

## 7. Ручная правка как часть архитектуры

Ручная правка — не временный костыль.
Это обязательный слой надежности.

### Зачем он нужен

- уведомления на Android не идеальны;
- разные производители и банки ведут себя по-разному;
- пользователь должен иметь контроль, если система ошиблась.

### Что должно быть можно сделать вручную

- подтвердить событие;
- исправить сумму;
- отметить событие как не относящееся к оплате смартфоном;
- удалить ложный дубль.

### Текущий минимальный контракт ручной правки

- `confirm` переводит событие в `CONFIRMED` и помечает `userEdited = true`;
- `correct amount` меняет сумму, переводит событие в `CORRECTED` и помечает `userEdited = true`;
- `dismiss` переводит событие в `DISMISSED` и помечает `userEdited = true`;
- каждое ручное действие пишет отдельную запись в `payment_event_edits`.

## 8. Обновление данных и реактивность

### Основной поток

1. Пришло новое уведомление.
2. Сохранили raw event.
3. Запустили parse / normalize / dedupe pipeline.
4. Обновили `PaymentEvent`.
5. Пересчитали summary.
6. Обновили экран и виджет.

### Важный принцип

UI и widget должны наблюдать **подготовленные данные**, а не самостоятельно инициировать пересчет бизнес-логики.

## 9. Privacy by design

### На уровне хранения

- храним только то, что реально нужно для продукта;
- по возможности не размазываем текст уведомлений по нескольким таблицам без необходимости;
- избегаем лишнего логирования чувствительных данных.

### На уровне UI

- private mode у виджета обязателен;
- текущий первый private mode реализован как один persisted toggle в приложении, а не как per-widget configuration;
- если дневной лимит не задан, private mode оставляет count-only summary подтвержденных оплат;
- если дневной лимит задан, private mode показывает только remaining-to-limit summary и count, без точной потраченной суммы;
- текстовые подсказки не должны раскрывать больше, чем нужно;
- экран с raw/debug данными — только developer/debug use case.

### На уровне продукта

- не отправлять финансовые данные на сервер по умолчанию;
- не добавлять аналитику, которая уносит содержимое уведомлений наружу.

## 10. Что не должно появиться незаметно

Ниже список дрейфов, которые выглядят безобидно, но ломают замысел проекта:

### 10.1 Прямой UI-доступ к сырым уведомлениям
Плохо, потому что доменная логика начинает течь в интерфейс.

### 10.2 Огромный универсальный parser
Плохо, потому что потом невозможно безопасно расширять поддержку банков.

### 10.3 Cloud sync втихую
Плохо, потому что ломает privacy-first позицию проекта.

### 10.4 Слишком ранний Settings overkill
Плохо, потому что приложение превращается в конструктор до того, как научилось базово считать деньги за день.

### 10.5 Попытка сделать универсальный финансовый продукт
Плохо, потому что проект теряет свою острую и понятную ценность.

## 11. Технические решения, которые можно менять без пересмотра ADR

Можно менять без отдельного архитектурного пересмотра:

- package names;
- точную структуру папок;
- формат mapper-ов и adapter-ов;
- UI toolkit details внутри Compose/Glance;
- детали DI;
- naming некоторых классов;
- точную форму DAO/API.

Нельзя менять без явного согласования:

- главный источник данных;
- local-first подход V1;
- наличие manual correction;
- privacy mode у виджета;
- отказ от обходных небезопасных способов получения данных.

## 12. Рекомендуемый путь реализации

### Шаг 1
Сначала научиться ловить и сохранять raw notifications.

### Шаг 2
Сделать простейший парсер и показать debug UI.

### Шаг 3
Сделать domain summary for today.

### Шаг 4
Вывести summary в widget.

### Шаг 5
Добавить manual correction.

### Шаг 6
Улучшать matching, dedupe и confidence.

На текущем этапе первый минимальный dedupe slice уже сделан, privacy slice и daily limit slice тоже уже реализованы.

Текущая limit/privacy связка выглядит так:

- глобальный persisted toggle живет на экране `Today`;
- отдельный `Settings` screen хранит один глобальный дневной лимит в `SharedPreferences`;
- `Today` и widget читают лимит через `DailySpendSummary`, а не считают прогресс самостоятельно;
- regular mode виджета показывает точную сумму, count и при наличии лимита заменяет `Last` на `Left/Over`;
- private mode скрывает потраченную сумму и показывает либо count-only fallback, либо remaining-to-limit summary, если лимит задан;
- `NoPermission` и `NoData` состояния остаются общими для обоих режимов.

Следующий рекомендуемый шаг после этого onboarding/privacy slice — расширение parser coverage для новых банковских уведомлений и точечные explainable confidence updates поверх уже существующего pipeline.

## 13. Что делать, если архитектура кажется недостаточной

Не менять ее в лоб.

Нужно:

1. сформулировать проблему;
2. показать, какой инвариант мешает;
3. предложить минимальное изменение;
4. обновить соответствующий ADR после подтверждения пользователя.

Это особенно важно для тем:

- новые источники данных;
- cloud sync;
- новые permissions;
- расширение продукта за пределы «траты смартфоном за день».
