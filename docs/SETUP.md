# SETUP.md

Этот файл описывает рекомендуемую настройку Codex для этого проекта.

## 1. Где запускать проект

Для этого проекта предпочтительный сценарий:

- Windows 11 как host;
- WSL Ubuntu 24.04 как dev environment;
- проект лежит в Linux filesystem внутри WSL;
- VS Code открыт из WSL командой `code .`;
- Codex запускается внутри терминала WSL.

Именно этот сценарий лучше всего соответствует текущему окружению пользователя.

## 2. Рекомендуемая настройка VS Code

Если используется Codex IDE extension на Windows, стоит включить запуск через WSL.

Рекомендуемая настройка VS Code:

```json
{
  "chatgpt.runCodexInWindowsSubsystemForLinux": true
}
```

## 3. Рекомендуемая глобальная настройка Codex

Вне репозитория полезно завести глобальные файлы:

- `~/.codex/AGENTS.md`
- `~/.codex/config.toml`

В `templates/global-codex/` лежат готовые примеры, которые можно вручную скопировать и адаптировать.

## 4. Docs MCP

Для OpenAI/Codex-вопросов полезно подключить OpenAI Docs MCP.

Команда:

```bash
codex mcp add openaiDeveloperDocs --url https://developers.openai.com/mcp
codex mcp list
```

Альтернатива — вручную добавить это в `~/.codex/config.toml`:

```toml
[mcp_servers.openaiDeveloperDocs]
url = "https://developers.openai.com/mcp"
```

Также полезно добавить в глобальный `~/.codex/AGENTS.md` такое правило:

```md
Always use the OpenAI developer documentation MCP server if you need to work with the OpenAI API, ChatGPT Apps SDK, Codex, or official OpenAI product behavior without me having to explicitly ask.
```

## 5. Что я бы НЕ включал сразу

### Hooks
Пока не делать их обязательной частью проекта.

Причины:

- hooks в Codex экспериментальные;
- Windows support у hooks сейчас временно отключен;
- для этого проекта больше пользы принесут хорошие docs, skills и аккуратные промпты.

### Агрессивные approval overrides
Не зашивать в repo слишком жесткие глобальные настройки approval/sandbox, если в этом нет явной причины.

Проектные настройки должны помогать, а не ломать пользовательский ритм работы.

## 6. Что полезно держать в `.codex/config.toml` проекта

В репозитории уже предусмотрен `.codex/config.toml` для проектных subagent-настроек.

Репозиторный config должен быть скромным:

- ограничения для subagents;
- project-specific defaults;
- без навязывания опасных прав доступа.

## 7. Git discipline

Перед заметной задачей полезно сделать checkpoint.

Минимальный ритм:

1. закоммитить чистую базу;
2. дать Codex узкую задачу;
3. посмотреть diff;
4. сделать review;
5. закоммитить результат.

## 8. Как запускать работу по шагам

Типовой цикл:

1. Открыть репозиторий в WSL.
2. Запустить `codex`.
3. Для нетривиальной задачи начать с `/plan`.
4. Дать узкий промпт из `docs/PROMPT_RECIPES.md`.
5. После реализации попросить отдельный review.
6. При необходимости обновить docs.

## 9. Что делать, если Codex не видит контекст

Проверить по порядку:

1. Запущен ли Codex из корня репозитория.
2. Есть ли `AGENTS.md` и читает ли его текущая сессия.
3. Не был ли открыт Codex слишком глубоко в поддиректории.
4. Не нужна ли новая сессия после добавления новых skills.
5. Доверен ли проект, если используются `.codex/config.toml`.

## 10. Что делать, если надо больше автоматизации

Сначала усилить:

- `AGENTS.md`
- ADR
- `PLANS.md`
- skills
- prompt recipes

И только потом думать о hooks или более тяжелой автоматизации.
