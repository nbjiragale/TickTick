# Existing feature correctness review

Date: 2026-10-13. Baseline: `35410a7`. Scope: existing behavior only; no new product features.

This is a source-level review plus APK build verification. It does **not** establish that every feature works on a phone. The repository's build-only restriction remains active: no unit, instrumentation, emulator or device tests were run. The scenarios below describe code paths and pending acceptance cases, not executed tests.

## Defects corrected

| Existing flow | Trigger and previous behavior | Correction |
| --- | --- | --- |
| Saved task navigation | A saved list/tag/filter is removed, or a crash interrupts deletion before the new destination is persisted. A missing list could be labelled Inbox while filtering on a nonexistent ID; adding a task could target that removed list. | Resolve missing destinations to the actual Inbox before selecting tasks, presentation preferences or the task-creation destination. Normalize tag casing against saved tags. |
| Tag rename/delete | A list name contains a tag-looking string, such as `#work`, and a task in that list also has the `work` tag. Whole-title regex replacement changed characters inside `~☰#work`, corrupting the list token. | Mask the task's list-token spans while locating tag tokens, then replace only real title-tag spans. Keep inline list text intact. |
| Backup export/recovery | Export allowed more than 100,000 records or a field longer than 1,000,000 characters, while restore rejected them. The pre-restore recovery file also lacked the normal 32 MiB limit. | Apply the same record/field limits during snapshot construction and the same byte limit before saving recovery. Reject unsupported snapshots before writing the destination or replacing data. Display validation/limit explanations in the backup dialog. |
| Startup attachment cleanup | Cleanup waited for repositories to load but the UI could already delete a task (creating a live Undo batch) or start restore before ownership was inspected. Old media could be removed while still needed by Undo or imported data. | Finish startup cleanup before opening the main UI. Read committed task/draft ownership and recovery references under the same database lock as writes/restore. Skip cleanup after a restore or when references cannot be read; cleanup failure does not block app availability. |
| Plan Your Day saves | While a task action was saving, menu actions remained callable. After swiping to another card, changing its menu could save an older planning snapshot over the transaction's reviewed-task checkpoint. | Guard menu, delete-dialog and cancellation mutations while the action commits, matching the existing guards on selecting, closing and other task actions. |

Changed implementation: `TasksViewModel.kt`, `TaskMutation.kt`, `BackupManager.kt`, `BackupSettings.kt`, `AppContainer.kt`, `TickTickApp.kt`, `PlanYourDayViewModel.kt`. No schema or dependency change.

## Coverage and remaining verification

All rows were traced through their UI/controller and repository/domain paths. “No additional defect established” means source review did not establish a further issue in this pass; it is not a runtime pass.

| Feature group | Paths reviewed | Result / device checks still needed |
| --- | --- | --- |
| Task creation/editing | `TaskEditorViewModel`, draft publication, autosave, NLP, checklist, repository save/revision checks | No additional defect established. Verify rapid typing, close during save, failed save, stale editor, process restart, date parsing and checklist preservation. |
| Lists/tags/filters/search | `TasksViewModel`, `TaskMutation`, DAO mapping, organization editors, search overlay | Destination and token defects fixed. Verify rename/delete with saved filters, duplicate names, title/description search, restart and new-task destination. |
| Batch actions and Undo | `TaskSelectionViewModel`, `TaskMutation`, ownership cleanup | Cleanup race fixed. Verify atomic multi-task edits, duplicate, delete/Undo with shared attachments, deletion after list removal and stale recurrence actions. |
| Recurrence and schedules | `TaskSchedule`, `TaskRecurrences`, schedule editor, mutation coordinator | No additional defect established. Verify month-end/leap-year recurrence, explicit dates, DST, completion-based dates, skip and exhausted series. |
| Calendar | `CalendarScreen`, occurrences/history projection, month/week/day/list and action routing | No additional defect established. Verify current vs projected occurrence actions, completed history, overlapping durations, week-start setting, date navigation and time labels. |
| Plan Your Day | Session persistence, task/checkpoint commit, menu handlers | Save race fixed. Verify rapid swipe/menu interactions during writes, error retry, restart and midnight rollover. |
| Habits/check-ins | Habit mutation/rules, dated configuration, editor, check-in UI, archive, goals and stats | No additional defect established. Verify partial/full quantity, Undo, weekly and finite goals, historical edits, archive/unarchive and completion feedback. |
| Habit/task reminders | Reminder projection/store, receiver/worker, platform PendingIntent identities, foreground and popup handling | Previous multiple-time fix retained. Verify each selected habit time after check-in, foreground log left open, background/locked phone, actions, pause/resume, restart/reboot, constant repeats and permission/channel/DND combinations. |
| Focus | ViewModel, Room session/history, controller, completion worker, screen lifecycle/audio | No additional defect established. Verify process death vs reboot, duplicate completion, pause/stop history, selected audio and keep-screen-on lifecycle. |
| Settings and backgrounds | Room preferences, local/sound settings, image grants, shared formatting, navigation wiring | No additional defect established. Verify persistence, cancellation/error handling in Android pickers, missing media grants, tab visibility, default list/time and week/time display. |
| Attachments | Import/capture, private ownership, opening/thumbnails, cleanup, duplicate/Undo references | Startup cleanup fixed. Verify cancelled/failed imports, unavailable viewers, old shared files, restart and storage exhaustion. |
| Backup/restore/startup | Staging validation, transaction, recovery file, restore generations, draft revisions, repository reload and startup gate | Export/recovery limits fixed. Verify round trip, rejected input with no data change, storage failure, interrupted restore, missing media and restored drafts. |

## What remains

The next work is acceptance verification of existing behavior, starting with reminders, data recovery and attachment Undo. No new feature implementation is proposed. The existing deferred feature list in `IMPLEMENTATION_AUDIT.md` remains outside this review.

Build results are recorded in the backend tracker. The APK is unsigned for release; device checks should use the debug build unless release signing is configured separately.

## Verification evidence

JDK 21 `:app:assembleDebug :app:assembleRelease -Pkotlin.incremental=false -Pkotlin.compiler.execution.strategy=in-process --console=plain` passed in 3m 52s (16 executed, 72 up-to-date), including release lint-vital checks. No Kotlin warnings; a non-fatal lint analytics warning concerns the sandbox's unwritable home directory. `git diff --check` passed. Local build log: `build/existing-features-review-build.log` (ignored). Both APK variants were generated. No runtime or device tests executed.
