# Backend implementation plan

Created: 2026-09-16. Scope: the offline Android backend in this repository.

## Session checkpoint

**Current state (2026-09-27):** B1-B5 remain implemented. A second B2/B4 popup repair adds Android 15/16 full-screen PendingIntent creator opt-in, lets Android decide full-screen vs heads-up presentation instead of omitting the intent based on an early lock-state snapshot, declares lock-screen/wake attributes at launch, and restores queued incoming requests across activity recreation. Settings now identifies popup-channel, Do Not Disturb and pause blockers. The four sound preferences and schema 5 remain intact. The user's exact device failure has not been reproduced; device popup/audio behavior and earlier runtime acceptance remain unverified.

**Next action:** install `app/build/outputs/apk/debug/app-debug.apk` for user device acceptance. In reminder settings check the bell pause, notification/exact-alarm access, both channels and popup special access. Create a new real task reminder a few minutes ahead and verify foreground, unlocked background and locked/screen-off delivery, followed by task actions, simultaneous task/habit reminders and recreation. Capture Android/device details and delivery logs if failure persists. Keep four-sound acceptance pending; resume B6 only after these requested corrections are accepted.

**Current milestone:** B2/B4 popup delivery repair implemented; debug assembly passed and runtime validation pending. B0 complete; B1-B5 implemented with validation pending; B6-B9 not started.

**Blockers:** no implementation blocker. Android channel overrides, phone volume, silent mode, Do Not Disturb and popup access remain authoritative. The agent's build-only verification boundary remains in effect.

**Working tree:** substantial pre-existing UI/source/reference changes are the baseline. Do not reset, clean, or overwrite them.

**Validation this session:** JDK 17 debug assembly passed in 3m 7s (9 executed, 29 up-to-date), with no Kotlin warnings. Command: `:app:assembleDebug -Pkotlin.incremental=false -Pkotlin.compiler.execution.strategy=in-process --console=plain` (both dotted property arguments quoted in PowerShell). Merged manifest confirms the non-exported popup activity and both lock-screen/wake attributes. Seven-file UTF-8/whitespace checks and `git diff --check` passed. APK: `app/build/outputs/apk/debug/app-debug.apk`; log: `build/popup-repair-build-final.log`. Initial invocation failed before compilation due to PowerShell argument splitting; no source failure. No unit/instrumentation/emulator/device tests were run.

**Scope decisions:** the user authorized implementation after the planning request. Use the existing offline/local product architecture. No server, account system, cloud sync, subscriptions, or network API is needed for V1. Backend implementation proceeds ahead of unfinished UI acceptance; it does not mark the UI gate complete.

### Start and end of each session

1. Read this checkpoint and root `AGENTS.md`; inspect `git status --short` and the files needed for the next milestone.
2. Read the relevant current reference notes. Prefer current user instructions and the latest dated corrections over older architecture text or memory.
3. Choose one bounded slice, its dependencies, and its acceptance checks. Record any changed decision here before expanding scope.
4. Reuse domain/repository boundaries; make only the UI changes necessary to connect durable state and report failures.
5. Before ending, update this checkpoint and the milestone table with actual results, remaining gaps, and the next concrete action. Never mark runtime acceptance passed from a successful build.

## Source of scope and current baseline

Read alongside [architecture](ARCHITECTURE.md), [product brief](personal_task_habit_app_project_details.md), [UI plan](UI_IMPLEMENTATION_PLAN.md), and [reference index](references/README.md). These contain historical entries; newer scope corrections take precedence.

Historical pre-backend baseline audited on 2026-09-16 under `app/src/main/java/com/niranjan/ticktick/` (the B1 implementation notes below supersede task storage entries):

| Area | Current implementation | Backend gap |
| --- | --- | --- |
| Composition | `app/AppContainer.kt` binds in-memory task, habit, and holiday repositories with a shared clock | Production database and adapter wiring |
| Tasks and organization | `domain/repository/TaskRepository.kt`, `data/repository/InMemoryTaskRepository.kt`; shared task/list/tag/filter snapshots, batch mutations, snooze and Undo | Durable aggregate writes, query observation, failures, concurrency and migration |
| Scheduling | `domain/model/TaskSchedule.kt` stores duration, reminder offsets, constant reminders and repeat configuration | Occurrence generation, history and actual delivery |
| Habits | `domain/model/Habit.kt`, `HabitRepository`, `InMemoryHabitRepository`; sections, settings, archive, quantity and completed-date maps | Durable check-ins, consistent derived completion, streak/goal rules and reminders |
| Editor and preferences | SavedStateHandle/saveable draft and presentation state; asynchronous attachment imports | Durable drafts/settings and save acknowledgement |
| Reminder UI | `feature/reminders/ReminderViewModel.kt` has session delivery previews and shared task actions | Receivers, persisted delivery state, notification actions and reconciliation |
| Focus | `feature/focus/FocusViewModel.kt` uses elapsed realtime and SavedStateHandle | Durable sessions/history, recovery and completion delivery |
| Attachments | `feature/taskeditor/AttachmentActions.kt` and `platform/attachments/AttachmentFileProvider.kt` | Managed ownership, missing-file recovery, cleanup and backup policy |
| Calendar | `feature/calendar/CalendarOccurrences.kt`; shared task/habit data and labeled sample holidays | Shared recurrence projection; a real holiday source remains unconfigured |
| Android configuration | Single `:app`, min SDK 24, target 36, compile 37.0; no Room/WorkManager dependencies in current Gradle files; manifest allows backup | Compatible persistence dependencies, platform components and deliberate backup rules |

No `graphify-out/graph.json` exists in this checkout; this plan uses direct source inspection.

### Behavior to preserve

- Keep Tasks, Calendar, Focus, Habits navigation and the supplied layouts. Latest notes include habits in Tasks **List** layout; Kanban remains task-only. Calendar includes tasks and habits. Older task-only home guidance is superseded.
- Preserve List/Kanban, per-destination presentation, custom lists/tags/filters, inline `#`/`~` behavior, task timestamps, pinning and atomic batch actions.
- Task Manage Section, View Options, Share and the removed sidebar management route stay excluded. Existing task section metadata/moves may still need storage; this does not authorize new management screens. Habit section management remains in scope.
- Templates, Quick Add Settings and Suggested Tasks remain deferred. Accepted provisional Focus dialogs remain valid.
- Snooze is an absolute deadline measured from selection time and does not replace the due date. Change Date invalidates obsolete snooze/delivery state.
- The bell uses an exclusive local resume date: pausing N days resumes on today + N. Persist the current effective setting once; do not recreate tomorrow's pause on every cold start.
- Retain debug fixture isolation and empty release initialization. A production database must never reseed samples over saved user data.

## Target architecture and storage contract

Use the existing single module, constructor injection and application container:

```text
Compose -> ViewModels -> domain rules/coordinated actions -> repositories -> Room
Android receivers/notification actions -> same domain actions -> repositories
Committed reminder state -> reconciliation -> AlarmManager / notifications
```

Room is the durable source of truth. Keep entities/DAOs in `data/local`, mappings in `data/mapper`, implementations in `data/repository`, pure recurrence/habit rules in `domain`, and Android integration in `platform`. Add packages only as they acquire code. No navigation/framework rewrite is required.

Task and habit repository writes are suspending, with explicit success/failure at callers and an exact committed Task returned from task save. Observable snapshots and a shared Loading/Ready/Failed state distinguish startup from an empty database. Database transactions run off the main thread; aggregate reads and serialized writes publish only committed snapshots. Task and habit revisions reject stale full-record saves and check-ins. One Room coordinator serializes task, habit, reminder and platform acknowledgement operations.

Proposed persisted records (final columns and indexes are a B1 deliverable):

| Records | Required invariants |
| --- | --- |
| Tasks, lists, tags, task-tag links, saved filters/rules, checklist items | Stable identity, foreign keys, explicit ordering, timestamps, note/task kind and status; normalized tag identity with existing title-token behavior preserved |
| Schedule, duration, repeat rules and explicit repeat dates | Preserve none/date-only/timed distinctions, repeat anchor and zone policy; stable serialization codes rather than enum ordinals |
| Occurrences and completion/history | Separate series and occurrence IDs; unique occurrence key; completing/skipping an occurrence advances at most once |
| Reminder definitions, deliveries and pending reconciliation | Owner/occurrence IDs, scheduled instant, snooze, revision, delivered/dismissed state, deduplication identity and recoverable pending work |
| Habits, sections, schedule revisions and check-ins | Unique habit/date check-in, quantity, archive state and order; derive completion/stats from one canonical history rather than two writable maps |
| Preferences and drafts | Typed, versioned values; per-destination preferences and alert resume date; editor session/owner ID and draft version |
| Attachments | Stable owner/file IDs, private relative path or persisted URI, MIME/size and availability; no bitmap/file bytes in saved UI state |
| Focus sessions | Mode, duration, phase, timestamps, accumulated elapsed time, task association, note and completion status |

Use foreign keys, targeted indexes and aggregate transactions. Keep schema exports under version control, implement explicit migrations, and never use destructive migration for user data. Store persisted product settings in Room as already specified by the architecture; avoid adding a second settings store without a concrete need.

## Delivery milestones

Statuses: **Not started**, **In progress**, **Implemented; validation pending**, **Verified**. A milestone is Verified only when its listed acceptance evidence exists. B0 is a documentation milestone.

| ID | Deliverable | Depends on | Status |
| --- | --- | --- | --- |
| B0 | Source audit, saved plan and session entry instructions | — | Verified (documentation only) |
| B1 | Room foundation and durable task/organization slice | B0 | Implemented; validation pending |
| B2 | Single task reminder end to end | B1 | Implemented, including September 27 popup repair; validation pending |
| B3 | Recurrence and occurrence actions | B1, B2 | Implemented; validation pending |
| B4 | Habit persistence, rules and reminders | B1-B3 | Implemented, including September 27 popup repair; validation pending |
| B5 | Durable preferences, drafts and remaining view integration | B1; B3/B4 for occurrences | Implemented, including four sound preferences; validation pending |
| B6 | Attachment ownership and recovery | B1, B5 | Not started |
| B7 | Durable Focus sessions and completion | B1, B2, B5 | Not started |
| B8 | Local export/restore and maintenance | B3-B7 | Not started |
| B9 | Production wiring and reliability acceptance | B1-B8 | Not started |

### B1 — Room foundation and durable tasks

- [x] Confirm compatible stable Room/KSP versions against the existing AGP/Kotlin/min-SDK setup; add only required dependencies and schema export configuration.
- [x] Define schema 1 for task aggregates, organization, schedule configuration and snooze, including revision and local-zone policy. Occurrence/delivery identities will be introduced with their first consumer in B2, then expanded in B3.
- [x] Implement DAOs/mappers and a Room task repository. Preserve IDs, order, checklist, tags, filters, attachments metadata and timestamps; transact bulk updates and delete/Undo restoration.
- [x] Adapt TaskEditor, Tasks, TaskSelection, Plan Your Day and Reminder callers to asynchronous results. Preserve drafts on errors, await save before closing, and reject stale full-task writes.
- [x] Wire the durable repository through AppContainer. Initialize Inbox once; keep demo fixtures separate from persisted user data. There was no existing on-disk task database to migrate from this session-only baseline.
- [x] Preserve one-session Undo until used/replaced or its ViewModel is destroyed. No attachment-file deletion/cleanup occurs in B1, including on delete/Undo/duplicate.

**Acceptance:** create/edit/complete/reopen a task after cold launch with all metadata; list/tag/filter results agree; failed writes retain drafts; batch actions are atomic; delete/Undo restores IDs, order and snooze; schema constraints and migration strategy have evidence. Build success alone leaves this milestone validation pending.

#### B1 implementation notes — 2026-09-18

- `data/local/TaskEntities.kt`, `TaskDao.kt`, `TickTickDatabase.kt`: ten tables with foreign keys, indexes, stable ordering and schema export. Schema 1 is saved at `app/schemas/com.niranjan.ticktick.data.local.TickTickDatabase/1.json`. Future versions require explicit migrations; no destructive fallback is enabled. An initialization marker and Inbox are committed in the same transaction.
- `data/mapper/TaskStorageMapper.kt`: relational checklist, tag links, attachments metadata and filter rules; versioned JSON for bounded reminder/repeat/duration value objects. Dates/times remain local ISO values, snoozes/timestamps remain instants, and enum names are persisted instead of ordinals. Task rows store `device_local` as the zone policy.
- `data/repository/TaskMutation.kt`: the existing task mutation rules extracted into a transaction-local helper shared by Room and the explicit in-memory demo repository. Existing batch semantics and timestamp behavior are retained; optimistic revisions prevent stale updates and deleted-task resurrection. Undo increments restored revisions while preserving prior metadata.
- `RoomTaskRepository.kt`: application-scoped, mutex-serialized transactions read the latest aggregate and publish after commit. Room invalidation refreshes external changes through the same lock. Writes finish once started; startup failure offers Retry without resetting data.
- `TaskRepository.kt`, `TaskStoreState.kt`, task/editor/selection/planning/reminder ViewModels and organization forms: suspend writes, cancellation-aware results, duplicate-submit guards, error presentation and success-dependent close/advance. The editor preserves typing during saves and can discard only its unsaved draft after a conflict. External reminder/status/pin fields can reconcile; conflicting content revisions are not silently overwritten.
- `app/AppContainer.kt` and `TickTickApp.kt`: real task storage in both debug and release, with a loading/retry gate. Fresh databases get only Inbox; existing debug seed files remain available but are not automatically imported. Habit and holiday repositories are still session/demo data. Focus, preferences and unfinished drafts have not gained durable storage in B1.
- `AndroidManifest.xml`: `allowBackup=false` disables Android cloud backup now that real user data is stored, consistent with the offline scope. Detailed device-transfer/extraction policy and user-controlled local export/restore remain B8 work; platform transfer behavior has not been tested.
- Dependency choices: [Room release notes](https://developer.android.com/jetpack/androidx/releases/room) and [KSP 2.3.6](https://github.com/google/ksp/releases/tag/2.3.6); compatibility checked through actual dependency resolution and debug compilation. Cloud backup setting follows [Android backup guidance](https://developer.android.com/identity/data/autobackup).

**Still unverified:** cold launch persistence, database failures, rollback, concurrent mutation behavior, real-device attachment access, migrations and visual behavior. Schema generation and compile success are the available evidence, not runtime acceptance.

### B2 — Smallest reliable reminder slice

- [x] Add occurrence/delivery identities and an explicit migration from schema 1. Persist one non-recurring task reminder and delivery/action state; persist the bell pause/resume preference before connecting delivery.
- [x] Add scheduler and notification adapters, channels, capability state, alarm/action receivers and deep-link entry into the existing task/reminder UI.
- [x] Commit desired schedule plus reconciliation marker in one transaction. Register/cancel outside it, mark applied only for the same revision, and retry pending reconciliation after interruption.
- [x] Give each occurrence/reminder offset a stable PendingIntent identity. Receiver/action handlers reload current state and ignore stale, deleted, dismissed or completed deliveries. Duplicate taps/delivery must be idempotent.
- [x] Wire Done, Close, Snooze and Change Date to the same durable actions as the foreground UI. Close stops this alert's re-alert cycle without completing the task.
- [x] Reconcile on launch, boot, app replacement, clock/zone changes and capability changes. Include a bounded missed-reminder recovery policy and honor pause at both scheduling and delivery time.
- [x] Surface notification permission/channel/exact-alarm capability accurately. Background fallback is an actionable notification; add full-screen presentation only if supported and eligible.

**Acceptance:** one task survives cold launch and produces a real reminder; each action updates the shared UI and durable state; duplicate/stale delivery is harmless; reboot and commit-before-registration interruption recover; denied/revoked permissions show a truthful state. Physical-device checks are needed for timing, Doze and lock-screen behavior.

#### B2 implementation notes — 2026-09-18

- `data/local/ReminderEntities.kt`, `ReminderMigration.kt`, `TickTickDatabase.kt`: schema 2 adds current occurrence identities, delivery/outbox rows and alert preferences. Explicit `MIGRATION_1_2` preserves schema 1 tables. Delivery rows deliberately survive task deletion until platform cancellation is acknowledged; cancelled offsets remain tombstones for their current occurrence so reconciliation cannot recreate them.
- `domain/repository/ReminderRepository.kt`, `data/repository/ReminderStore.kt`, `RoomTaskRepository.kt`: reminder scheduling state commits atomically with task writes. Schedule/snooze changes replace occurrence identities; content edits retain the occurrence and invalidate action revisions. Dismissal stops the delivery without completing the task. Platform effects are applied outside the database transaction under the same write mutex, then acknowledged only for their current revision. Interrupted effects remain dirty for reconciliation.
- `platform/reminders/ReminderPlatform.kt`, `ReminderController.kt`, `ReminderReceiver.kt`, `ReminderMaintenance.kt`: stable immutable PendingIntents; notification Done, Snooze 15 min, Change Date and swipe-dismiss; foreground card queue; launch/resume, boot, app update, time/zone and notification/exact-access reconciliation. Receiver commands are durably enqueued before immediate processing, with revision checks for duplicate retries. WorkManager 2.11.2 supplies deferrable retry/maintenance, while AlarmManager owns deadlines. No full-screen/background activity launch was added.
- Timing decisions: device-local wall-clock due times, date-only default 09:00, absolute snooze deadlines captured at selection/tap time, including delayed retries. Constant reminders use 15-minute intervals with at most four follow-ups after the first alert and a 24-hour cutoff. A delivery more than two minutes late becomes a silent missed reminder with no repeat cycle; recovery retains the latest due offset per task and posts one silent summary. The in-app settings explain constant cadence and missed recovery. These are implementation policies, not device timing guarantees.
- The bell's exclusive resume date is stored in Room; a prior SavedState value is imported once when available. Fresh settings retain the previous default of paused until tomorrow, initialized once. Pausing cancels active effects; a local-midnight wake, foreground date refresh and maintenance reconcile resumption. Old reminders catch up silently. Snooze remains separate from original due time and remains recorded until an existing task action invalidates it.
- `MainActivity.kt`, `TickTickApp.kt`, `AppOverlays.kt`, `ReminderViewModel.kt`, `ReminderSettings.kt`, `TasksViewModel.kt`, `DailyAlertsToggle.kt`: notification entry uses delivery revision checks and missing/stale feedback; existing card actions await durable results. Permission/channel/exact-alarm state is visible with explicit settings/permission actions. Exact access absent uses an inexact alarm fallback and explains possible delays. Debug card previews remain explicit, but their task mutations (including snooze) now affect the real task backend.
- `app/AppContainer.kt`, `core/time/DeviceClock.kt`, manifest, notification drawable and Gradle catalogs wire the adapters into the existing app. The clock reads the current device zone. Recurring tasks currently deliver only their stored due occurrence; completion still completes the task until B3 implements advancement/history. Habit reminders and Focus delivery remain later milestones.
- Static schema inspection confirms all ten schema 1 table definitions are unchanged and each new schema 2 table/index SQL matches `MIGRATION_1_2`. This is not a runtime migration test. Schema exports 1 and 2 are retained.
- Platform references checked: [alarms](https://developer.android.com/develop/background-work/services/alarms), [notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission), [notification block-state broadcasts](https://developer.android.com/reference/android/app/NotificationManager), [WorkManager releases](https://developer.android.com/jetpack/androidx/releases/work).

**Still unverified:** migration execution, notification/action delivery, duplicate/interrupted processing, pause expiry, permission revocation, reboot, time/zone changes, Doze, lock-screen behavior and visual acceptance. Build-only verification remains in effect. Force-stopped delivery is not promised.

### B3 — Recurrence and occurrence identity

**Implementation decisions (2026-09-21):** Due-date rules advance one scheduled occurrence per Complete/Skip, retaining overdue dates until explicitly handled. Daily/weekly intervals use a fixed anchor (Monday-based interval weeks); monthly/yearly calculations retain the original day/month to avoid clamping drift. Completion-based rules use the actual local completion date; weekly rules choose the first selected weekday on/after completion + N weeks. Early completion can move the next date earlier than the original future due date. Skip uses the scheduled date as its completion-rule base. Specific dates are sorted/deduplicated and finite; exhaustion completes/declines the final record. DST gaps shift forward by the gap and overlaps use the earlier offset, saved in history. Schedule edits start a new series revision and current occurrence, while title/content edits retain identity and historical records remain unchanged. Calendar projects only due-date/specific-date futures; completion-based futures are unknown. Projected/history entries cannot complete the live occurrence. No future database task rows are generated; per-date projection is capped at 512 overlapping occurrences.

- [x] Implement pure generators for due-date, completion-based and explicit-date rules, with injected clock/zone and bounded calendar queries.
- [x] Define month-end/leap-year anchors, DST gaps/overlaps, completion-relative intervals and overdue catch-up behavior before implementing them. Initial proposed policies are recorded below.
- [x] Store history and schedule revision; atomically complete/skip once and calculate the next occurrence. Reject repeated actions on an already-advanced occurrence.
- [x] Connect Skip the Recurrence only when its semantics work; share occurrence projection between task queries, planning, calendar and scheduler.
- [x] Define how editing a series affects future occurrences while preserving historical completion. Separate schedule validation for new reminders from loading/editing existing overdue records.

**Acceptance:** deterministic cases cover weekdays, intervals, explicit dates, January 31, leap day, DST, delayed completion, duplicate completion, skip and schedule edits. No duplicate advancement or unbounded future-row generation.

#### B3 implementation notes — 2026-09-21

- `domain/model/TaskRecurrence.kt`: shared pure next-date calculation, device-zone due-time resolution and bounded per-date projections. Anchor-based month/year arithmetic preserves January 31 and leap-day intent; interval weeks start Monday. Completion-based rules use the injected clock; Skip uses the due date. Duration dates shift together, retaining wall-clock times/all-day state. Calendar does not predict unknown completion dates. Reminder/time/duration-only edits retain the original date anchor; changing the due date or repeat pattern resets it.
- `data/local/TaskRecurrenceEntities.kt`, `TaskDao.kt`, `TickTickDatabase.kt`, `data/mapper/TaskStorageMapper.kt`: schema 3 adds backend-owned recurrence state and immutable occurrence history. Migration 2-to-3 creates only these two tables/indexes. Task foreign keys cascade on deletion; delete/Undo now carries recurrence metadata and history. Version 2 schedules receive initial identities in the startup transaction; versions 1 and 2 retain their migration chain.
- `TaskMutation.kt`, repository implementations and `TaskRepository.kt`: Complete/Skip atomically record one historical outcome, advance the current task, reset checklist completion for its next occurrence and clear snooze. Due-date repeats advance one scheduled slot, including overdue slots. Explicit-date exhaustion marks the final task completed (or declined for Skip). Every completion requires the displayed task revision; stale full-task and bulk saves fail. Reopening a finished series assigns a new current occurrence identity and preserves historical outcomes; reversing prior history is not a new UI action in this slice.
- `ReminderStore.kt`: occurrence identity participates in alarm invalidation, including a next occurrence with the same date after early completion. Schema-2 fingerprints are adopted once so dismissed deliveries survive upgrade. Notifications/cards use the shared completion transaction; the next committed due date is reconciled by the B2 scheduler. Validation, history and reminders share domain timezone resolution.
- Tasks, planning, debug reminder completion, editor and bulk completion pass revision-checked requests. The editor merges external advancement/checklist reset without carrying an old completion gesture into a new occurrence. Existing **Skip the Recurrence** is enabled for active repeating-task selections and executes atomically.
- Calendar uses shared projections in every view and includes Completed/Skipped history when Show Completed is enabled. Projected/history checkboxes are disabled with explanatory accessibility text; tapping opens the current series in the existing editor. Current occurrence completion remains available. Stable keys distinguish overlapping durations/history. Tasks and planning expose the one committed actionable occurrence, avoiding synthetic editable task rows.
- Schedule validation distinguishes unchanged overdue reminders from newly assigned schedules. Existing overdue tasks can be edited/completed without removing reminders; changed/new expired reminders are rejected. Automatic advancement permits an overdue next occurrence. Offset/count/duration constraints are checked before saving.
- Date behavior follows explicit app policies using Java [LocalDate arithmetic](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/LocalDate.html) and [local-to-zone resolution](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/LocalDateTime.html). No new dependency was introduced.

**Pending acceptance cases (not executed):** January 31 -> February clamp -> March 31; leap-day return; interval weekdays; early/late completion; explicit-date sorting/exhaustion; DST gap/overlap; overlapping durations; duplicate Complete/Skip; notification/editor races; schedule edits with history; delete/Undo; schema 1/2 upgrades; restart and real recurring alarms. Build-only verification remains in effect.

### B4 — Habits

**Implemented policies:** dated quantities are canonical; completion is derived using the configuration effective on that date. Existing schedule/amount/unit/goal changes apply from the next local day, or next Monday when either the old or new frequency is weekly. Pending revisions at or after the replacement's effective date are removed atomically. Earlier eligibility and targets remain unchanged. Name, section, recording mode, reminder times and presentation settings apply immediately. The editor explains the schedule effective date.

Weekly periods start Monday; partial weeks require at most their unarchived eligible days. A weekly streak counts successful weeks; an unfinished current week stays pending. Daily/interval streaks count scheduled opportunities and leave today's unfinished opportunity pending. Switching between weekly and daily/interval starts a new current/best streak era because their units differ; total completed days remain lifetime totals. Finite goals count completed dates, freeze the achieved streak, stop subsequent delivery, and reopen after Undo. Backdated check-in/Undo recomputes these derived values. Archive immediately cancels delivery and records the local day's archive state while retaining quantities; same-day archive/unarchive resolves to the latest state. Unscheduled/archived opportunities do not break streaks. Fresh production habit storage is empty with default sections/settings only.

- [x] Replace InMemoryHabitRepository with durable records, sections, settings and canonical quantity history; preserve archive/restore and section ordering.
- [x] Implement daily/weekday, interval and weekly-target eligibility, binary/amount progress, finite goals and scheduled-opportunity streaks.
- [x] Preserve historical schedule meaning when a habit is edited; define how backdated check-in/Undo recomputes completion, goals and streaks.
- [x] Generate multiple habit reminders through the B2 pipeline; honor archive, achievement, pause, selected sound and dismissal. Share records with Habits, Tasks List and Calendar.

**Acceptance:** check-in/Undo and archive/restore survive restart; duplicate check-ins cannot double count; weekly/incomplete-week, unscheduled-day and finite-goal cases are correct; all views agree; achieved/archived habits cancel obsolete reminders.

#### B4 implementation notes — 2026-09-24

- Room schema 4 adds `habits`, `habit_schedule_history`, `habit_checkins`, `habit_sections` and `habit_settings` with explicit migration 3-to-4. Habit/date is the check-in primary key; child history/check-ins cascade on deletion. Configuration JSON is versioned and uses stable enum names. Targeted writes, history replacement and reminder outbox changes commit together. No debug samples enter the production database in either variant.
- `RoomTaskRepository.habits` supplies the existing HabitRepository contract through the same database, mutex and startup gate. UI writes await acknowledgement; editor/manual quantity/section-add errors retain input, and stale habit revisions reject lost updates. Completion is a derived property, with no second writable completed-date map. Draft restoration beyond saveable state remains B5.
- Habit reminders use owner keys `habit:<id>:<epochDay>` and per-local-time delivery keys. Task-shaped inputs are transient adapters for B2 only; they never enter task storage or task lists. Today plus seven days are projected, with a persisted next-midnight maintenance wake so longer interval habits still acquire future alarms. Cold-start catch-up covers today only; older habit days do not produce notifications. Local times resolve DST gaps forward and overlaps to the earlier offset.
- Reminder-time edits create a new definition identity and exclude already-past times. Completion, archive, weekly/finite achievement suppress obsolete deliveries while preserving dismissal tombstones. Undo/unarchive restores future eligible times without re-alerting earlier suppressed times. Multiple times share B2's latest-due catch-up and bounded constant policy: initial alert plus at most four 15-minute follow-ups, with no follow-ups for missed alerts. Opening, dismissal, check-in or Undo ends the active day's repeat cycle; later configured times remain eligible if the day is incomplete.
- Habit notifications offer Check in for binary/automatic amount habits, Open habit and Dismiss. Manual amounts open the existing detail entry. Foreground Auto pop-up opens that same habit/date and acknowledges the alert without checking in. It does not launch an activity from the background. Missed habit alerts use a separate silent summary and exclude automatic pop-up.
- Selected sound/silent and badge preference choose stable Android channels; system overrides and launcher support still apply. TickTick Pop has no bundled asset and uses the system notification default. The settings screen states these limits. Channels follow the official [Android channel guidance](https://developer.android.com/develop/ui/compose/notifications/channels); badges follow [Android badge guidance](https://developer.android.com/develop/ui/views/notifications/badges). Sound/badge behavior has not been tested on a device.
- Changed paths under `app/src/main/java/com/niranjan/ticktick/`: `data/local/{HabitEntities,TickTickDatabase}.kt`, `data/mapper/HabitStorageMapper.kt`, `data/repository/{HabitMutation,InMemoryHabitRepository,RoomTaskRepository,ReminderStore}.kt`, `domain/model/{Habit,HabitRules,HabitReminder}.kt`, `domain/repository/{HabitRepository,ReminderRepository}.kt`, `feature/habits/{HabitsScreen,HabitEditor,HabitCheckInScreen,HabitManagementScreens}.kt`, `platform/reminders/{ReminderPlatform,ReminderController}.kt`, `feature/reminders/ReminderSettings.kt`, `app/{AppContainer,TickTickApp}.kt`, and `MainActivity.kt`. Also changed debug `HabitSeed.kt`, schema export `4.json`, this plan, architecture and habit references.
- Static schema comparison confirms all 15 schema-3 tables are unchanged and all five new CREATE statements match Room's schema-4 export. Build evidence is recorded in the checkpoint. **Acceptance remains pending:** no database migration/restart, domain execution, device, alarm, sound, badge, gesture or visual tests were run under the build-only constraint.

### B5 — Preferences, drafts and query integration

**Implementation decisions:** use Room schema 5 for versioned UI preferences and editor drafts behind the existing startup gate. Preserve presentation and completed visibility per destination, plus Calendar mode/date and same-day planning resume. Restore task/habit drafts with owner/session/version and base revision; task/habit save clears the acknowledged draft in the same transaction. Retain newer edits and report conflicts rather than silently rebasing over changed records. Persist only applied child-picker values; unfinished picker choices remain transient. Interrupted attachment imports recover as an explicit retry message without a phantom attachment. Keep selections, menus, animations and confirmation dialogs transient. Continue build-only validation.

- [x] Persist per-destination List/Kanban/background/group/sort/detail/completed preferences, supported habit settings and useful planning resume state. Keep transient animation/selection state local.
- [x] Add versioned draft persistence preserving NLP suppression, manual schedule choices, attachment import state and sheet/full-screen identity; discard removes the draft, successful save clears it after commit.
- [x] Finish shared durable queries for search, counts, tags/filters, Today/Upcoming and Calendar. Refresh date/zone-sensitive state at midnight and resume.
- [x] Keep sample holidays clearly labeled; leave release holidays empty until a source and region are selected. No implicit online holiday lookup.

**Acceptance:** cold restart preserves intended preferences/drafts; saved and cancelled child choices behave correctly; no false empty state while loading; midnight/zone changes refresh results without divergent copies of records.

#### B5 implementation notes — 2026-09-25

- Schema 5 adds `ui_preferences` (key, format, JSON payload) and `editor_drafts` (key, session, version, owner, base revision, format, nullable payload), with explicit migration 4-to-5. Stable enum names and format versions replace implicit serialized-object persistence. All 20 previous tables are unchanged; both new CREATE statements match Room's schema export in static comparison.
- `RoomUiStateRepository` observes these tables and gates startup with Loading/Ready/Failed. Preferences publish only after commit. Draft writes run in an ordered application-owned queue, survive composition/ViewModel disposal, retain failed work for Retry and expose a storage error. Task/habit save waits for queued draft writes and clears only its acknowledged session/version in the record's transaction. Task-editor deletion also clears its acknowledged draft atomically. Nullable payloads are tombstones preventing delayed writes from restoring discarded/saved versions. A replaced editor's later publications cannot replace the new session.
- Task List/Kanban, detail visibility, grouping, sorting/direction, backdrop/swatch/image URI and completed visibility persist under destination keys for Today, lists, tags and saved filters. Selected task destination/home section also persist. Existing image-provider grants and unavailable-image feedback remain in use. Habit settings reuse B4's table. Calendar mode, selected date and completed visibility persist independently; selecting Today follows the current local day, while explicitly browsing another date preserves it.
- One task draft and one habit draft can be recovered. Task payload version 2 retains text/selection, checklist, attachment metadata, note/status, manual schedule, NLP enabled/suppressed expression, resolved prediction date/time and sheet/full-screen identity. Relative predictions keep the previously resolved date when the recognized expression is unchanged. Unapplied child choices are not promoted to the parent draft. Existing unchanged task details need no recoverable draft; successful autosave retires that version while later typing remains recoverable. A newer draft left after an interrupted save retains its old base revision and requires conflict review instead of duplicating/overwriting a task.
- Attachment bytes and jobs are not serialized. An interrupted import restores a retry message and retains earlier attachments; it never creates a phantom successful import. Managed file ownership and provider recovery remain B6. Habit drafts retain the configuration and editor step using the same versioned habit codec as storage. Invalid draft payloads are retained and expose an explicit recovery/discard message. Full discard queues a tombstone; storage failures remain visible and retryable.
- Planning persists candidate order, reviewed IDs, selected ID, local review date and original recurring occurrence IDs. Reopening on the same day resumes without automatically opening the planning dialog on launch. Done, Won't Do, date changes and Delete commit the task and next review checkpoint together. Earlier navigation writes drain first; advanced recurring occurrences are excluded from the old review. A new local day/review starts fresh candidates. Menus, delete prompts and animations remain transient.
- Existing search, drawer counts, tag/filter queries, Today/Upcoming and Calendar were checked against their shared Room-backed snapshots. Calendar retains B3's read-only future/history projection; task lists continue to act on current task records. Tasks refresh date and zone on resume and at most every 30 seconds (with a midnight wake); Habit home consumes that shared date, and Calendar's Today selection follows it. Planning refreshes on resume and resets an active prior-day review. Debug holidays remain explicitly labeled samples; release holidays remain empty without a selected source/region.
- Changed paths under `app/src/main/java/com/niranjan/ticktick/`: new `data/local/UiStateEntities.kt`, `data/repository/RoomUiStateRepository.kt`, `domain/repository/UiStateRepository.kt`, `domain/model/HabitSerialization.kt`, and `feature/tasks/TaskPreferencePersistence.kt`; updated database/migration registration, task/habit repository contracts/adapters, habit storage mapper, `app/{AppContainer,TickTickApp}.kt`, task presentation/ViewModel, task editor state/persistence/ViewModel/host, Habit editor/home, Calendar screen and Planning ViewModel. Added schema export `5.json`; updated this plan, architecture and affected reference notes.
- **Validation:** compilation/debug assembly and static schema/source inspection only; final build evidence is in the checkpoint. No restart, process-death, failed-storage, midnight/zone, migration, device, emulator, unit or instrumentation execution was performed. B5 is implemented, not runtime-verified.

### B6 — Attachments

- [ ] Retain durable URI access where possible; otherwise copy to app-private storage with temporary files and commit ownership after a successful import.
- [ ] Handle interrupted imports, unreadable/deleted providers, storage exhaustion and cancelled drafts without saving broken references as successful imports.
- [ ] Track shared ownership for Duplicate and Undo; delayed orphan cleanup must not delete another task's attachment. Restrict FileProvider paths and URI grants.
- [ ] Keep recording/scanning expansion outside this slice unless separately requested; persist current supported imports first.

**Acceptance:** image/file/audio attachments remain accessible after restart; failure is recoverable; duplicate/delete/Undo preserve valid media; unreferenced temporary files are eventually cleaned safely.

### B7 — Focus persistence

- [ ] Introduce a Focus repository for session configuration, notes, task association and history. Preserve current Pomo/Stopwatch counting and foreground-only sound/screen-on behavior.
- [ ] Persist elapsed baselines and a reboot-aware recovery policy; never compare an old boot's elapsedRealtime value with a new boot's counter.
- [ ] Schedule completion through a suitable supported Android adapter, cancel/update on pause/stop, and record completion once even if UI and alarm race.
- [ ] Derive statistics from history; task deletion must not corrupt historical focus records. Background audio/service expansion is a separate product decision.

**Acceptance:** rotation, tab switches, process recreation, pause/resume, stop and reboot follow documented semantics; completion is counted once; existing notes/history survive restart. Sound and screen-on remain device-verification items.

### B8 — Backup, restore and maintenance

- [ ] Export a versioned JSON snapshot via user-selected local storage, including durable records/settings/history. Document that V1 JSON includes attachment metadata, not media bytes.
- [ ] Validate schema version, field values, IDs/references, sizes and file availability before restore; reject unsupported newer formats without changing current data.
- [ ] Implement deliberate replace-all restore with an in-app confirmation and pre-restore recovery snapshot. Apply database changes transactionally, then invalidate old alarms and reconcile restored state.
- [ ] Use WorkManager only for deferrable cleanup/reconciliation/backup maintenance. Honor offline-only scope in manifest cloud-backup and extraction rules; decide device-transfer behavior explicitly.

**Acceptance:** round trip retains IDs, schedules, history and preferences; malformed/interrupted restore leaves prior data usable; missing media is disclosed; cancelled restore has no side effects; restored alarms do not duplicate old deliveries.

### B9 — Production completion

- [ ] Verify production AppContainer uses durable task/habit/focus/settings/draft stores and real adapters; keep simulator controls and sample seeding isolated from release.
- [ ] Complete migrations, error/loading/retry handling and safe diagnostics without logging private titles, notes or media.
- [ ] Complete the validation matrix below and record device/OS/build evidence. Close remaining product decisions and document known platform limits.

**Acceptance:** all approved backend journeys work offline across restart and upgrades, with no known data-loss path; reminder reliability has device evidence; unresolved verification is listed explicitly. UI screenshot acceptance remains separately tracked.

## B2/B4 popup correction — 2026-09-25

**Diagnosis:** the user confirmed that the notification arrives but the popup does not. Static tracing found no background popup activity, overlay permission, full-screen intent or popup settings. The existing card was only composed in MainActivity; the habit auto-open effect could also acknowledge a delivery while that activity was paused. A received notification rules out total delivery failure for that occurrence; it does not prove the phone's channel importance, Do Not Disturb state or special access settings.

**Implementation and decisions:**

- `platform/reminders/ReminderPlatform.kt`: fresh task alerts and habits with Auto pop-up enabled can launch the translucent reminder activity over an unlocked app using user-granted `SYSTEM_ALERT_WINDOW`, Android's documented background-activity exception. Locked/non-interactive devices use a notification full-screen intent only when access is available. This uses an Activity with its own lifecycle, not a persistent overlay service. The existing notification stays available if popup launch is denied or suppressed.
- Popup launches require a high-importance channel and Do Not Disturb off. Silent recovery, ordinary reconciliation and permission refreshes do not launch old popups. Pre-26 task notifications now set sound/vibration defaults. Existing channels are inspected and linked to settings rather than recreated to override user choices.
- `platform/reminders/ReminderPopupActivity.kt`, `AndroidManifest.xml`, `res/values/themes.xml`: non-exported, translucent, separate task excluded from Recents; lock-screen/turn-screen-on flags; current delivery/revision validation after Room readiness; queued incoming intents; restoration waits for storage; successful actions/invalidated alerts close the window and return to the underlying app. No keyguard bypass, foreground service, new dependency or database migration.
- `feature/reminders/ReminderViewModel.kt`: popup instances only include specifically accepted deliveries. Additional reminders enqueue without discarding current Snooze input. The same durable Complete, Dismiss, Snooze and Change Date operations are reused.
- `feature/habits/HabitCheckInScreen.kt`: reuse the existing habit log and manual/automatic quantity controls in the popup, without management actions in this transient window. Existing per-habit Auto pop-up choices are preserved. Check-ins use the shared revision-checked repository; Close dismisses the current reminder.
- `app/TickTickApp.kt`: reminder intent handling, task card presentation and habit auto-open require RESUMED lifecycle, preventing a paused main screen from consuming background habit alerts. `ReminderController.kt` exposes main-screen visibility for handoff and reconciles due reminders every 30 seconds while that screen is foreground, even if exact alarm access is unavailable.
- `ReminderReceiver.kt`: a WorkManager enqueue failure/timeout no longer skips immediate alarm/action handling. Durable/periodic recovery remains available.
- `feature/reminders/ReminderSettings.kt`: Settings and the drawer bell's existing reminder controls show over-other-apps access, Android 14+ full-screen alert access and task/habit channel importance, with direct settings actions and habit Auto pop-up guidance. The settings content scrolls on smaller displays. Android still owns granting special access; the app does not grant permissions itself.
- Reference updates: `docs/references/REMINDER_REFERENCES.md`, `docs/references/HABIT_REFERENCES.md`, `docs/ARCHITECTURE.md`, and this tracker. Prior daily-alert pause choices and the excluded reminder Focus action are unchanged.

**Validation:** initial JDK 17 debug build passed (2m 13s); final build after all source edits passed (1m 33s, 5 executed and 33 up-to-date, no Kotlin warnings). Merged manifest confirms both popup permissions and the non-exported, singleTask, separate-affinity activity excluded from Recents. UTF-8 and whitespace checks passed for all 14 changed source/resource/document files; `git diff --check` passed. APK: `app/build/outputs/apk/debug/app-debug.apk`; final log: `build/reminder-popup-final.log`. Device/visual verification, denied/revoked access, locked-screen/OEM behavior, simultaneous reminders, rotation/process recreation, DND, and actions remain acceptance work for the user's device testing. Debug UI previews remain foreground simulations; use a new real due reminder to verify background delivery.

**Platform references checked:** [Android background activity exceptions](https://developer.android.com/guide/components/activities/secure-bal), [notification full-screen intents and channels](https://developer.android.com/develop/ui/compose/notifications/create-notification), [Android 14 full-screen intent access](https://source.android.com/docs/core/permissions/fsi-limits). Full-screen access does not guarantee Android will open an activity in every device state.

## B2/B4 popup delivery repair — 2026-09-27

**Diagnosis and limits:** the user reports popup delivery still failing. Static inspection found no creator background-launch opt-in on the full-screen PendingIntent despite target SDK 36; full-screen intent attachment also depended on a lock-state snapshot taken before notification presentation. The activity declared lock-screen/wake behavior only during creation, and incoming requests awaiting processing were not saved across recreation. Settings inspected notification importance but omitted the independent popup-channel importance and current DND/pause blockers. These are code-path gaps, not a reproduced diagnosis of a specific phone; the device/Android version and precise failure state have not been supplied.

**Implementation and decisions:**

- `platform/reminders/ReminderPlatform.kt`: explicit immutable full-screen intents grant creator background-launch privileges on API 35 and use `MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS` on API 36+. The grant is limited to the explicit reminder activity PendingIntent supplied to Android's notification system. Fresh eligible notifications attach this intent whenever full-screen access is available. Android decides whether to launch the activity or show a heads-up notification at presentation time; unlocked custom-card launches still require overlay access. Existing channel importance, DND, pause, habit Auto pop-up, stale revision and silent recovery checks remain authoritative.
- `AndroidManifest.xml`: declare `showWhenLocked` and `turnScreenOn` on the non-exported popup activity before creation; retain runtime flags for older Android.
- `platform/reminders/ReminderPopupActivity.kt`: save and restore the complete incoming delivery/revision queue, including requests not yet processed before rotation/recreation. Accepted and handled state and ViewModel restoration remain in use.
- `platform/reminders/ReminderController.kt`, `feature/reminders/ReminderSettings.kt`: expose persisted pause and independent task/habit popup-channel importance; explain active pause/DND/disabled-popup-channel blockers in the existing settings surface. No permission is automatically granted, no stored pause is reset, and no channel override is bypassed.
- `docs/references/REMINDER_REFERENCES.md` and this plan describe the revised route. No schema/dependency change or reminder-card redesign.

**Validation:** final JDK 17 debug assembly passed in 3m 7s (9 executed, 29 up-to-date), with no Kotlin warnings. Merged manifest and seven-file UTF-8/whitespace checks passed, as did `git diff --check`. APK: `app/build/outputs/apk/debug/app-debug.apk`. The first invocation failed before compilation because PowerShell split an unquoted dotted Gradle property; the rerun quotes both `-P` arguments. Logs: `build/popup-repair-build.log` and `build/popup-repair-build-final.log`. No unit, instrumentation, emulator or device tests run under the existing build-only restriction. Device acceptance must cover Android 14/15/16, granted/denied popup access, screen-state transitions, task/habit queues, recreation, actions and sound.

**Official references:** [Android background activity launches and creator opt-in](https://developer.android.com/guide/components/activities/secure-bal), [ActivityOptions launch modes](https://developer.android.com/reference/android/app/ActivityOptions), [system full-screen vs heads-up behavior](https://source.android.com/docs/core/permissions/fsi-limits). Platform permissions and OEM behavior can still suppress automatic presentation; a successful build does not establish device success.

## B2/B4/B5 sound settings — 2026-09-25

The user requested four separately configurable sounds in Settings using Android's pre-installed tones.

**Implemented behavior and decisions:**

- Settings -> Sounds shows Task completion, Notification, Habit completion and Reminder popup. Each row opens `ACTION_RINGTONE_PICKER` with `TYPE_ALL`, System default and Silent. Cancel/missing result leaves the saved value intact; failed saves retain the prior value and show feedback. System default follows Android's notification default. A selected concrete URI retains the selected tone. No new media/storage permission is requested.
- Versioned URI/label records reuse Room's `ui_preferences` through `RoomUiStateRepository`, under four independent keys. No new table, dependency or migration. Notification sound is shared by task and habit notifications; the older habit settings entry now edits the same choice. An existing habit ringtone is the fallback for that shared choice until changed. Completion defaults stay Silent to preserve the previous lack of completion audio; Popup defaults to System default.
- Task/habit mutation helpers record an actual completion transition. `RoomTaskRepository` emits a callback only after successful commit, including notification actions and recurring/bulk task completion. A batch plays once. Partial habit progress, Undo, recurrence Skip, startup observation, duplicate/stale actions and failed writes do not produce completion feedback. Habit completion means crossing the dated quantity target.
- `AppSounds` plays one bounded, non-looping tone for completion/in-app popup feedback, observes ringer/DND state and catches unavailable-tone errors without failing the saved action. Playback is capped at five seconds. Foreground habit auto-open owns its popup tone to avoid racing the acknowledgement that closes the reminder; manual notification taps do not replay that tone.
- Background notifications own background reminder audio. Popup-capable delivery uses the Popup tone once, rather than layering it with the Notification tone; notification-only delivery uses Notification. Sound-specific task/habit notification and popup channels handle Android's immutable channel sound behavior. Existing default/legacy channel identities are preserved where applicable; Android overrides still apply to an existing channel. Permission/maintenance refreshes remain non-alerting, and missed summaries remain silent. Reminder reconciliation waits for preference storage on cold start and refreshes channel bindings when either reminder tone changes.
- Settings provides links to the current task/habit notification and popup channels. The main Settings content scrolls so the four new choices and existing controls stay reachable.

**Changed paths:** `domain/model/AppSound.kt`; `platform/sounds/AppSounds.kt`; `feature/settings/SoundSettings.kt`; `data/repository/TaskMutation.kt`, `HabitMutation.kt`, `RoomTaskRepository.kt`; `app/AppContainer.kt`, `AppOverlays.kt`, `TickTickApp.kt`; `feature/habits/HabitsScreen.kt`, `HabitManagementScreens.kt`; `platform/reminders/ReminderPlatform.kt`, `ReminderController.kt`; `feature/reminders/ReminderSettings.kt`; this plan, `docs/ARCHITECTURE.md`, and reminder/habit reference notes.

**Validation:** initial JDK 17 debug assembly passed in 1m 8s (7 executed, 31 up-to-date). Final assembly after all source edits passed in 1m 3s (5 executed, 33 up-to-date), with no Kotlin warnings. UTF-8 and whitespace checks passed across all 18 changed source/document files; repository-configured `git diff --check` passed. The final source review also added explicit silent updates so leaving the foreground does not replay a previously delivered reminder. APK: `app/build/outputs/apk/debug/app-debug.apk`; final log: `build/sound-settings-final.log`. Picker availability, actual audio, settings persistence after restart, channel switching, silent/DND behavior and simultaneous reminder/action playback remain unverified on a device. No unit/instrumentation/device/emulator tests were run.

**Official API references:** [Android ringtone picker](https://developer.android.com/reference/android/media/RingtoneManager), [notification-channel sound behavior](https://developer.android.com/reference/android/app/NotificationChannel). Android supplies the available tone list and can override app-selected notification sound through its channel settings.

## Decisions to finalize at the relevant milestone

These are proposed defaults, not claims about the original TickTick app. Resolve from existing references and user direction; ask only when a consequential product choice remains ambiguous. Do not block B1 on later-stage choices.

| Decision | Proposed starting point | Resolve by |
| --- | --- | --- |
| Time semantics | Due times/habit reminders follow local wall time; snoozes use absolute instants; retain a zone policy | B1/B2 |
| Month end and DST | B3: clamp while retaining original date anchor; shift gaps forward; earlier overlap offset, saved in history | Implemented; runtime validation pending |
| Constant reminders / missed reminders | Initial + at most four 15-minute follow-ups; >2-minute lateness is silent catch-up, latest due offset per owner/day and separate task/habit summaries; persist dismissal | B2/B4 implemented; device verification pending |
| Habit history | Schedule revisions preserve past eligibility; unscheduled days do not break streaks; incomplete current week stays pending | B4 |
| Focus reboot | Preserve accumulated time/history and recover to a documented safe state; decide paused recovery versus wall-clock continuation | B7 |
| Backup media / transfer | JSON metadata-only V1; ZIP media backup later; deliberate device-transfer policy | B8 |

## Validation and evidence policy

The current carried-forward constraint permits compilation/debug APK assembly only. This planning request does not authorize running unit/instrumentation/emulator/device tests. The checks below are future acceptance requirements; if the restriction remains when implementing, record them as pending and complete authorized build work. A later explicit testing instruction supersedes that boundary.

| Layer | Required future evidence |
| --- | --- |
| Pure domain | Fixed clock/zone recurrence, snooze, eligibility, streak, goal and parser preservation cases |
| Database | Aggregate round trips, foreign keys, unique keys, atomic batches, concurrent edits/actions, migrations, failed writes and restore rollback |
| UI integration | Save acknowledgement/failure, draft recovery, count/view consistency, notification deep links and missing-record handling |
| Platform/device | Notifications denied/channel off; exact access absent/revoked; foreground/background/locked; Doze; process death; reboot; zone/time change; pause expiry; missed and duplicate deliveries |
| Release/offline | No demo reseed, empty first launch with Inbox, offline operation, backup/extraction policy and upgrade preservation |

Do not equate force-stop with process death or promise delivery while force-stopped. Verify current platform behavior before claiming reliability. Follow official guidance for [alarm scheduling](https://developer.android.com/develop/background-work/services/alarms) and [notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission); exact alarms and notifications have separate capability requirements. [Room documentation](https://developer.android.com/training/data-storage/room) informs the persistence layer; choose compatible versions at implementation time. Sources reviewed 2026-09-16; recheck when adding dependencies/platform behavior.

Known prior build recipe (recheck local JDK 17 configuration when needed):

```powershell
.\gradlew.bat :app:assembleDebug '-Pkotlin.incremental=false' '-Pkotlin.compiler.execution.strategy=in-process' --console=plain
```

## Session history

| Date | Work | Evidence / next action |
| --- | --- | --- |
| 2026-09-16 | B0: inspected current sources, scope corrections and architecture; created this plan and root session instructions | Documentation only; next implementation slice is B1. No application code changed by this task. |
| 2026-09-18 | B1: implemented Room task/organization persistence and async UI integration after the user's implementation instruction | See B1 notes for changed paths and decisions. Final JDK 17 debug build passed (2m 21s); schema 1 exported; source checks passed. No runtime tests. Next: B2 durable reminder state and migration. |
| 2026-09-18 | B2: implemented durable task reminders, pause preference, schema 2 migration, platform alarms/notifications, shared actions and recovery | See B2 notes for changed paths and numeric delivery policies. Final JDK 17 debug build passed (1m 24s); static migration SQL/export comparison and source checks passed. No runtime tests. Next: B3 recurrence/occurrence actions; B1/B2 device acceptance remains pending. |
| 2026-09-21 | B3: implemented recurrence math, schema 3 history/identities, revision-checked Complete/Skip, next-alarm reconciliation and calendar projections | Final JDK 17 debug build passed (1m 25s); schema 2-to-3 static comparison and source checks passed. B3 notes list policies/paths and pending acceptance. Next: B4 habit persistence and rules. |
| 2026-09-24 | B4: implemented Room habits/sections/settings, canonical quantity history, effective schedule rules, goals/streaks and shared habit reminder delivery | JDK 17 debug assembly passed (2m 42s); final rebuild passed (11s). Static schema 3-to-4 and source checks passed. Runtime acceptance remains pending. Next: B5 durable presentation preferences and versioned drafts. |
| 2026-09-25 | B5: implemented schema 5 UI preferences/drafts, per-destination settings, task/habit recovery, atomic planning checkpoints and resume/date/zone refresh | Final JDK 17 debug build passed (1m 35s). Static schema 4-to-5 and source checks passed. Runtime acceptance remains pending. Next: B6 attachment ownership and recovery. |

| 2026-09-25 | B2/B4 correction: task/habit background popup window, special-access settings, lifecycle and receiver fixes | Final JDK 17 debug build passed (1m 33s); manifest/UTF-8/whitespace checks passed. User device acceptance is next, then resume B6. |

| 2026-09-25 | B2/B4/B5 addition: four saved Android sound-picker choices, commit-based completion feedback and separate notification/popup audio | Final debug build passed (1m 3s, no Kotlin warnings); 18-file UTF-8/whitespace and git checks passed. User device/audio acceptance is next. |

| 2026-09-27 | B2/B4 repair: modern full-screen launch opt-in, presentation-time routing, launch attributes, request restoration and settings blockers | Debug APK assembly passed (3m 7s, no Kotlin warnings); merged manifest and seven-file source checks passed. Exact phone failure not reproduced; user device acceptance remains next. |

For subsequent entries record: milestone/slice, changed paths, decisions, exact checks and outcomes, unresolved risks, and next action. Keep the checkpoint at the top current so a new session does not need to reconstruct the history.
