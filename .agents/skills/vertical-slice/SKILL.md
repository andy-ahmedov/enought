---
name: vertical-slice
description: Use when the user asks for an incremental implementation step, a small reviewable diff, or a plan for the next thin slice. Do not use for broad open-ended brainstorming.
---

This skill keeps work incremental and reviewable.

## Read first

1. `AGENTS.md`
2. `PLANS.md`
3. the relevant architecture or ADR document for the target area

## Slice design rules

A good slice:

- has one clear outcome;
- touches as few layers as possible;
- can be verified locally;
- does not require finishing the whole architecture to be useful;
- leaves the repository in a coherent state.

## Preferred slice types for this repo

- one domain model cluster
- one repository flow
- one parser rule
- one widget state
- one screen without extra settings
- one manual correction action

## Anti-patterns

Avoid slices like:

- “implement the whole MVP”
- “build the full parser system for all banks”
- “finish notifications, widget, onboarding, and storage together”

## When asked to implement

Respond in this order:

1. restate the narrow scope;
2. list what is explicitly out of scope;
3. propose a short plan;
4. make the change;
5. report touched files and verification steps.
