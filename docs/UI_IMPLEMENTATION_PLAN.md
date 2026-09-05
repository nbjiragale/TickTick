# End-to-end UI implementation plan

Status: first UI increment in progress, 2026-09-05. The complete milestones below are not yet marked done; Today/drawer implementation details are tracked in [reference notes](references/README.md).

Read alongside [the architecture](ARCHITECTURE.md) and [the product brief](personal_task_habit_app_project_details.md).

## 1. What UI first means

Deliver a native Compose app that can be installed and used through complete task and habit journeys with session data. All screens share observable in-memory repositories, so a change in one screen appears everywhere else. Screens use the state and navigation contracts that will remain when Room and system services are connected.

Build real navigation, editing, validation, filtering, sorting, checklist reordering, NLP feedback, habit input, and state transitions. Use deterministic platform simulators for alarm delivery, permissions, media capture, and backup operations. Simulator controls live in a debug-only scenario catalog; simulated system results must be clearly identified there and never claim real alarms or files were created.

An individual screen is complete when its entry points, controls, exits, content states, keyboard behavior, and shared-data effects work. A screenshot with disconnected buttons does not meet this definition.

## 2. Screen and interaction inventory

The brief section numbers provide traceability. Items described as provisional are architecture decisions where the brief gives a label without full behavior; adjust against reference evidence when available.

| Area / brief sections | Surfaces and actions | UI milestone evidence |
| --- | --- | --- |
| App shell, 4–6 | Four tabs, drawer, local profile area, search/settings/notification shortcuts, Today/Inbox/lists, counts, FAB | Tab stacks/scroll survive switching; drawer navigation and counts agree with shared data |
| Inbox and Today, 6, 23 | Task cards, times, priority, checklist/reminder indicators, completed/overdue sections | Create, complete, undo and reopen a task; filtering reflects changed dates |
| Task overflow, 7 | View, Background, Show Details, Show Completed, View Options, Group & Sort, Manage Section, Select, Share | Each supported choice changes presentation/session settings; selection supports meaningful bulk operations; Share opens a text preview and system chooser only on user action |
| Quick Add, 8 | Sheet above keyboard, title/description, date, priority, tag, list, more, voice, send | Draft survives pickers and full-screen promotion; only one task is created |
| Quick Add More, 8 | Image, Template, Convert to Note, Full-Screen, Settings | Image chooser and preview; select/apply a local template; note mode; same draft full screen; task-entry settings sheet |
| NLP, 9–11 | Live date/time and recurrence spans/chips, tags, `~` list trigger, priority menu | Exact examples from brief; chip suppression; cursor/IME intact; manual selection precedence |
| Task Detail, 12–14 | List, priority, more, date/reminder/snooze, title, description, checklist, tags, attachments | Existing edit and new full-screen creation share content; checklist edit/check/reorder works |
| Attachments, 14 | Take/Choose Photo, Records, File, Scan Documents; preview/remove/missing-file state | Deterministic selection/capture success, cancellation, and failure states; no real recording/capture during UI-only phase |
| Date/time, 15–16 | Date and Duration tabs, calendar, start/end values, time clock and keyboard input, AM/PM, Clear | Valid selections round-trip; Cancel restores prior values; invalid intervals cannot Apply |
| Reminders, 17–18 | None, on time, offsets, custom days/hours/minutes, constant reminder | Labels and validation reflect chosen values; expired trigger warning; configuration persists in session |
| Repeat, 19 | None, daily/weekly/monthly/yearly/weekdays; custom basis, interval, weekdays, specific dates | Every supported rule can be edited/reopened without data loss; conflicting choices cannot coexist |
| Reminder and snooze, 20–22 | App-wide bottom card, date/list/title/description, Close, Complete; every snooze option | Simulate delivery from any tab; complete updates task views; snooze state appears in Task Detail; multiple alerts queue |
| Suggested Tasks, 24 | Upcoming candidates and `+` move-to-Today action | Moving a task changes Today and removes it from suggestions without duplication |
| Plan Your Day, 25 | One task at a time, progress, Done/Today/Later/Won't Do/Delete, finish/empty state | Each action advances once and updates underlying task state |
| Lists and tags, 26–27 | Create/edit, name/color, list view selection, validation, management, tag filtering | New choices appear in drawer/pickers; deleting a list moves tasks to Inbox after confirmation; deleting a tag removes associations |
| Habits home, 28, 33 | Weekly strip, selected date, sections, rows, empty state, add; section add/reorder | Date/section changes and check-ins update the same habit records |
| Habit basics, 29 | Name, icon, letter avatar, quote/refresh, Next; optional preset gallery | Required custom creation works; seeded quotes stay offline; Back preserves wizard input |
| Habit setup, 30–35 | Daily weekdays, weekly target, interval; binary/amount goal; goal days; start date; section; multiple reminders; auto-pop-up | Invalid targets/dates are blocked; saved habit reopens with identical settings |
| Habit check-in, 36–38 | Full-screen background/illustration/title/quote/control, back/more, achieved state, confetti, stats | Check-in/undo affects selected date and stats; repeat taps do not double count; reduced-motion path |
| Calendar, 4 | Minimal calendar/date selector with agenda using shared task rows | Select date, inspect tasks, open detail; no separate calendar data store |
| Focus, 4 | Minimal local duration selection, start/pause/reset and finished UI | Foreground session state works; background timing/recovery is deferred and explicitly scoped |
| Settings, 39–46 | Tab Bar, Appearance, Date & Time, Sounds & Notifications, Widgets, General, Integrations & Import, Backup | Controls affect the demo where applicable; platform-dependent states are reachable through fixtures; export/restore includes preview, cancel, progress, success and errors |

## 3. Scope decisions and missing references

- Today and navigation drawer screenshots have been provided and copied into `docs/references/`. Inbox and subsequent editor/picker flows still need their own screenshots. Record provisional dimensions and behavior; do not mark visual parity complete without comparisons.
- Build list view throughout V1. Show Kanban/Timeline as unavailable future options only where needed to reproduce the reference selector; never pretend switching to one provides that view.
- Calendar and Focus receive the small usable surfaces listed above. Advanced scheduling views, statistics, and production background focus timers are outside this first UI pass.
- Habit presets are optional; custom creation is required. Templates in Quick Add use a small local selectable set, with no template-management subsystem initially.
- `Records` in the attachment menu is provisionally interpreted as audio recordings. Camera, voice capture, file access, and scanning need real platform adapters after the UI gate. Document scanning must ultimately work offline; a remote OCR requirement must not be introduced.
- Home View Options provisionally controls density and visible metadata; Group & Sort supports due date/list/priority/manual order; Background offers local theme backgrounds; Manage Section creates/renames/reorders task sections.
- Task Detail More provisionally includes duplicate, checklist/note conversion, move, and delete. Habit More supports edit and undo check-in. Preserve entered data during conversions; confirm any lossy conversion.
- Plan Your Day: Done completes; Today moves due date to today while preserving an explicit time; Later opens date selection; Won't Do skips the current recurring occurrence or marks a one-off task declined; Delete confirms removal. Progress is measured against a stable session candidate list and tolerates external deletion/completion.
- Date-only reminders use a configurable default time, proposed 09:00. Tomorrow Morning uses that setting, Next Hour chooses the next whole hour, and Tomorrow preserves the due time or uses the default. Show the resolved date/time before applying. A date move that would put a reminder in the past requires a new time or no reminder.
- Settings is bounded to meaningful local controls: tab visibility/order, theme, week start/time format/default reminder time, reminder preferences/capabilities, widget preview, general confirmation/default-list choices, local import and backup. Widgets' actual installation and remote integrations are deferred. Keep Tasks accessible if tab preferences change.
- Notification shortcut provisionally opens a local reminder center showing active, snoozed, and missed alerts; it is not a social inbox. The profile area represents a local user with no login.

## 4. Implementation order

Each milestone produces a runnable increment. Complete its interactions before adding the next large feature.

### UI-0 — Foundation and reference catalog

- Verify baseline Gradle build and establish emulator/device targets.
- Add compatible stable Navigation 3, lifecycle Compose/ViewModel support, serialization, coroutines, and time desugaring as needed. Retain existing tooling unless compatibility requires a change.
- Implement theme tokens and system icons; disable dynamic colors for the reference palette.
- Add the app container, clock/zone seams, basic domain models, shared store, and initial repository contracts.
- Add a debug component/scenario catalog and deterministic fixture clock. Default demo time: 2026-09-05 09:00 Asia/Kolkata, with advance-time controls for midnight and reminders.
- Add the four-tab shell, drawer, inset handling, and saveable tab stacks.

**Exit:** app launches in the intended theme; all tab/drawer routes open; a task row, sheet, and input render in previews and on the device. Build success is recorded, not assumed.

### UI-1 — Tasks and organization

- Build Inbox/Today/custom-list task views and shared row/checklist indicators.
- Implement complete/undo, list/tag creation and selection, counts, sections, sorting/grouping, view preferences, selection and sharing preview.
- Build search across task title/description, with results and no-results states.

**Exit:** editing fixture records through available actions updates every affected list/count/filter without navigation resets.

### UI-2 — Task entry and shared editor

- Build Quick Add and full-screen Task Detail from shared editor state/content.
- Add date/duration/time, reminder, repeat, priority/tag/list pickers, checklist editing/reordering and attachment flows.
- Add live NLP, suppression behavior, new-tag flow, notes/templates and simulated voice input.
- Implement validation, dirty dismissal, save failure/retry, duplicate-save protection and editor restoration across rotation.

**Exit:** type `Bring groceries tomorrow at 10 am #shopping`, inspect NLP, dismiss its chip, set a manual date, promote to full screen, add checklist/attachment, save once, reopen and edit. Every value and cursor-sensitive interaction behaves consistently.

### UI-3 — Reminder experience and daily planning

- Build reusable reminder card, all snooze choices, occurrence queue, and local reminder center.
- Inject task and habit reminder deliveries from the scenario catalog while any tab or editor is open.
- Add permission-denied, notification-channel-disabled, exact-alarm-unavailable, and restricted-popup fixtures.
- Build Suggested Tasks and Plan Your Day, including finish and no-candidate states.

**Exit:** a simulated reminder can be completed, dismissed, or snoozed; updated state appears in task detail and Today. Interrupted drafts survive. All planning actions produce the specified shared-data changes.

### UI-4 — Habits end to end

- Build date strip, sections, custom habit wizard, icon/avatar/quote choices, frequency, goals, start date, reminders and edit mode.
- Build full-screen check-in, amount entry, achieved state, celebration, stats and undo.
- Implement pure schedule/check-in rules required by these interactions with focused correctness tests; leave actual alarm registration behind the simulator.
- Cover selected weekdays, weekly target, interval, finite goal, before-start and already-achieved scenarios.

**Exit:** create a habit, change selected date, check in, undo, edit it, and inspect consistent history/statistics. Ineligible dates and duplicate actions are handled deliberately.

### UI-5 — Remaining surfaces and settings

- Complete the minimal Calendar and Focus journeys.
- Build the bounded settings above and wire local preferences to their UI effects.
- Complete attachment, permission, widget-preview, local import and backup/restore presentations using deterministic adapters.
- Review every visible overflow item, shortcut and wizard control against the inventory.

**Exit:** every in-scope control has a defined action and result. Deferred capabilities are explicit, and simulated success cannot be mistaken for completed device work.

### UI-6 — Full UI acceptance

- Walk all journeys below and capture failures with route, scenario and expected behavior.
- Verify small/large phone widths, gesture and three-button insets, keyboard open/closed, landscape and larger fonts. Avoid introducing tablet-specific layouts unless required by the actual target device.
- Compare supplied reference images at matching dimensions/theme/font scale and correct shared tokens before individual screen overrides.
- Verify TalkBack labels/roles, focus order, touch targets, contrast, reduced motion and accessible alternatives to drag-only actions.
- Produce the debug demo APK and a short scenario guide listing actual verification evidence and outstanding reference/platform work.

**Exit:** interactive UI gate passes. Visual fidelity remains pending if source images are still unavailable. No production alarm or persistence claim is inferred from this gate.

## 5. Cross-screen acceptance journeys

| Journey | Required observation |
| --- | --- |
| Create and organize | Add a list/tag, create a dated task, find it in correct list/Today/search, change its list and verify counts |
| Draft preservation | Quick Add → date picker → Cancel → full screen → Back; no duplicated task or discarded input |
| NLP override | Detect expression → dismiss chip → type unrelated text → chip stays suppressed → change date expression → parse again; manual date wins |
| Checklist | Add/edit/check/reorder items, leave and reopen; row indicator agrees with detail |
| Reminder interruption | Open dirty editor → inject reminder → snooze → return; draft intact and snooze visible on the target task |
| Repeated actions | Rapid Send/Complete/check-in taps; one resulting task/occurrence/check-in and stable UI |
| Planning | Move a suggested task to Today; execute every planning action; progress and final data agree |
| Habits | Custom creation → frequency/goal/reminders → save → check-in/undo → date switch → statistics; no future or unscheduled accidental check-in |
| Failure recovery | Simulate failed save, missing record, missing attachment, denied capability and invalid backup; preserve recoverable input and provide useful next action |
| State restoration | Rotate in editor, picker and habit wizard; change tabs and return; no draft/selection loss or repeated side effects |
| Demo restart | Restart the process; fixtures reset as documented, with no claim of durable user data |
| Settings | Change time format/week start/tab configuration; visible UI updates and navigation remains usable |

## 6. Verification strategy

Use component previews for quick visual inspection, Compose interaction tests for the important journeys, and focused unit tests for NLP precedence, date validation, snooze resolution, recurrence boundaries and habit eligibility/streaks as those rules are implemented. Prefer tests of observable behavior and data consistency over getters or one assertion per menu item.

Stable screenshot cases: populated/empty Today, Quick Add with keyboard and NLP, Task Detail with checklist/snooze, date/duration sheet, repeat editor, reminder/snooze, habit list/setup/check-in/achieved, and settings. Pin clock, locale, font scale, theme and animations for comparisons.

During implementation, compile and run relevant checks after each meaningful increment. At the UI gate, run the accumulated UI suite and the acceptance walkthrough. Add Room transaction/migration/backup tests and physical-device reminder checks in the later gates; simulator results are not evidence of those capabilities.

## 7. First production work after the UI gate

Connect one complete durable task/reminder path: Room task/reminder state → exact-alarm capability → scheduling → receiver → notification/in-app card → Complete/Snooze → reboot and process-death reconciliation. Test it on the intended phone immediately.

Then replace the remaining in-memory repositories, finish durable attachments and backup/restore, and wire habit reminders. Retain the debug scenario catalog so visual and failure-state testing stays fast after production integration.
