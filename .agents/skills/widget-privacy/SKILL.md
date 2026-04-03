---
name: widget-privacy
description: Use when building or reviewing the widget, lock-screen visibility, UI summaries of financial data, private mode, or permission / disclosure flows. Do not use for backend parsing work.
---

This skill protects the widget from becoming leaky, noisy, or product-confused.

## Read first

1. `AGENTS.md`
2. `docs/adr/0002-local-first-and-privacy.md`
3. `docs/ARCHITECTURE.md`
4. `README.md`

## Product goal

The widget is not a mini bank app.
It is an at-a-glance surface that makes spending noticeable.

## Mandatory widget rules

- Support a privacy-preserving mode.
- Do not show more financial detail than needed by default.
- Handle at least these states: permission missing, no data, ready, and needs review.
- Read prepared summary state, not raw notifications.
- Keep the copy calm and non-judgmental.

## UX checks

Before approving a widget change, ask:

- Does this help the user notice spending in under 2 seconds?
- Does it overshare sensitive information on the home screen?
- Does it need a better zero state?
- Does it explain missing permissions clearly?
- Would a stranger glancing at the screen learn too much?

## Output style

When reviewing, lead with privacy or clarity problems first, then polish.
