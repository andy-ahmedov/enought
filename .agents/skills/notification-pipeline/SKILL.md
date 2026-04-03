---
name: notification-pipeline
description: Use when implementing or reviewing notification ingestion, raw event storage, parser rules, normalization, matching, dedupe, or confidence logic. Do not use for generic UI-only tasks.
---

This skill is for the phone-spend ingestion pipeline.

## Read first

1. `AGENTS.md`
2. `docs/ARCHITECTURE.md`
3. `docs/adr/0001-hybrid-data-source.md`
4. `PLANS.md`

## Pipeline rules

Preserve this flow:

`NotificationListenerService -> RawNotificationEvent -> parser -> PaymentCandidate -> normalization -> matching/dedupe -> PaymentEvent -> summary`

## Mandatory constraints

- Save raw events before adding clever interpretation.
- Keep parser logic out of UI and widget layers.
- Keep each parser rule narrow and explainable.
- Prefer multiple parser rules over one giant parser.
- Confidence must be explainable.
- Low-confidence events must have a review path.

## When implementing

Always ask:

- What raw fields are actually available?
- What is the smallest parser contract that works?
- Is this logic in the correct layer?
- Can this be tested with static notification examples?
- Does this change accidentally double-count one payment?

## When reviewing

Look specifically for:

- hidden coupling between Android notification APIs and domain logic
- parser logic placed in repositories or UI view models without boundaries
- weak dedupe logic that may inflate daily totals
- silent assumptions about certainty
- lack of test fixtures for sample notifications
