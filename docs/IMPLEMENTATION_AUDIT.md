# Offline implementation audit

Audit date: 2026-10-13. Baseline: `80c2158`. Scope follows the current backend plan and reference exclusions. Build success does not establish device acceptance.

| Area | Baseline finding | Delivery |
| --- | --- | --- |
| Tasks, recurrence, habits | Room persistence, revision checks, reminder reconciliation already implemented | Preserve existing behavior; device acceptance pending |
| Focus | SavedStateHandle only; history and background completion not durable | Schema 6 session/history, elapsed-realtime recovery, completion alarms and worker recovery |
| Backup | No export/import workflow | Versioned JSON, staging database validation, atomic replacement, pre-restore recovery copy, repository reload |
| Settings | Sounds supported; appearance/date/navigation defaults absent | Room-backed navigation, accent, week/time format, task defaults |
| Organization | Lists, tags, filters could only be created | Transactional edit/delete, task reassignment, token/filter updates |
| Search | Title only | Search title and description |
| Habit feedback | No completion celebration | Confetti on committed transition; reduced motion respected |
| Build delivery | No documented reproducible setup or CI | README and debug/release APK build workflow |

## Acceptance still required

The repository's build-only restriction remains active. None of the following is claimed as executed:

1. Upgrade real schema 1–5 databases to schema 6 without losing tasks, history, preferences or media.
2. Focus: pause/resume, process death, reboot, clock changes, denied notification/exact-alarm access, duplicate completion, stopped Stopwatch and interrupted Pomo history.
3. Backup: export/restore round trip; reject malformed/oversized/foreign-key-invalid input; simulate storage failure; verify recovery copy, stale drafts/actions, absent media and paused imported Focus.
4. Organization: rename/delete with tasks, inline tokens and saved filters; stale editor conflict; Undo after list deletion; restart persistence.
5. Settings: navigation ordering/visibility, week boundaries, time labels, default list/time, theme and restart persistence.
6. Reminders: supported Android versions, screen states, sound/channel overrides, DND, actions, recurring deadlines and simultaneous habit/task delivery.
7. Attachments: restart, missing provider permissions, cancelled import, duplicate/Undo, cleanup and recovery-copy references.
8. UI: small screens, large fonts, accessibility, rotation, reduced motion and reference screenshot comparison.

## Explicitly deferred product scope

Templates, Quick Add Settings, Suggested Tasks, task Manage Section/View Options/Share and the removed sidebar management route remain excluded by existing references. Timeline layout, in-app audio recording/automatic document scan, real widget installation, remote calendar integrations, accounts/cloud sync, transferable media archives and background Focus audio are not implemented. Production holidays remain unconfigured. Focus audio and keep-screen-on apply while the Focus screen is active.
