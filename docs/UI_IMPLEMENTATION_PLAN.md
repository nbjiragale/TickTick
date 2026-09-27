# End-to-end UI implementation plan

Backend planning update (2026-09-16): backend work is tracked separately in the [backend implementation plan](BACKEND_IMPLEMENTATION_PLAN.md). Read its session checkpoint first in new sessions; this UI checklist remains the record of UI scope and acceptance, not backend completion.

Timer increment — 2026-09-12: the supplied idle/running references replace the Timer placeholder with Pomo/Stopwatch, progress ring, expanded controls, screen-always-on and focus notes. The user approves provisional functional dialogs for duration, statistics, task selection, sound and More. See [Timer notes](references/TIMER_REFERENCES.md). Existing four-tab navigation is retained; backend background delivery and durable history remain deferred. Build-only verification applies.

Task selection increment — 2026-09-11: long-press and overflow Select now open multi-selection in List/Kanban with an animated five-action toolbar. Date, section/list moves, batch delete with yellow Undo, and More actions update shared session tasks. See [selection references](references/TASK_SELECTION_REFERENCES.md) for exact behavior, excluded Share and deferred recurrence/shortcut customization. This supersedes older "Select pending" entries below. Runtime/visual acceptance remains pending.

Notification pause update — 2026-09-11: long-press the home bell toggle for Enable/Disable for today or Custom. Custom takes a day count and previews the exact resume date before Apply; the UI is off until that date and then returns to on. This supersedes the earlier default-off-at-each-midnight behavior. Actual notification/alarm/popup handling remains deferred. See [task-view notes](references/TASK_VIEW_REFERENCES.md).

Scope correction — 2026-09-11: the user skips the sidebar's bottom-right management screen. Remove its icon and obsolete manageLists route; list/tag/filter editing, deletion and reordering through that management screen are no longer upcoming UI work. The bottom-left Add menu and existing list/tag/filter navigation remain in scope.

Drawer Add increment — 2026-09-09: bottom-left Add opens List/Filter/Tag. Full-screen List and Tag creation follow the supplied references, with name, preset/custom colors, save/cancel and List/Kanban selection. Local Normal/Advanced filter creation uses the reference example without premium gating; saved tags/filters have live drawer counts and task results. Timeline and undefined More content remain deferred. See [drawer-add notes](references/DRAWER_ADD_REFERENCES.md). Runtime/visual acceptance remains pending.

Snooze feedback — 2026-09-08: after a successful preset/custom snooze, show a compact confirmation such as "We'll remind you in 15 minutes." for two seconds. Date-based choices display the resolved day/time. Feedback survives the reminder closing or advancing its queue; backend delivery remains deferred. See [reminder notes](references/REMINDER_REFERENCES.md).

List habits / daily alert toggle — 2026-09-08: the user now requests habits alongside tasks in home List view, superseding the earlier task-only rule for that layout. A shared-data Habit card follows tasks; Kanban remains unchanged. A sliding-bell daily-alert switch sits left of Plan Your Day, defaults off and stores an enabled local date in UI state. The next day is off; notification/alarm/popup backend integration remains explicitly deferred. See [follow-up notes](references/TASK_VIEW_REFERENCES.md).

Show Details correction — 2026-09-08: detailed task rows now align the completion/note icon with the title, place date/time and the right-aligned list on one baseline, and use the reference's tighter spacing and larger date text. Removed the unreferenced extra metadata line. See [task-view notes](references/TASK_VIEW_REFERENCES.md); runtime/visual acceptance remains pending.

Latest task-view increment — 2026-09-08: View (List/Kanban), Background (None/Color/Gradient/Image), Show Details and Group & Sort follow the five supplied screenshots. Preferences are stored per task destination; creation/modification timestamps support their sort choices. Task Manage Section is removed from scope, and View Options/Share are explicitly excluded by the user. Select remains pending. These references supersede the earlier list-only decision. Debug APK assembly passed; runtime/visual acceptance remains unverified. See [task-view notes](references/TASK_VIEW_REFERENCES.md).

Latest increment — 2026-09-08: the Calendar tab implements List, Day, Week and Month views in `feature/calendar` over the shared task/habit repositories plus a new explicitly labeled sample-holiday source. Soft blue/green/amber blocks with icons and a legend distinguish tasks, habits and holidays; the selected date and view survive tab switches and rotation; Today/previous/next navigation adapts per view. Task blocks open the shared editor and habit blocks open check-in for the tapped occurrence date. Recurrence expansion remains deferred. Compilation/debug assembly passed; runtime and visual acceptance remain unverified. See [calendar notes](references/CALENDAR_REFERENCES.md).

Previous increment — 2026-09-07: Task Detail now shows "Snoozed until…" below the original due-date/time row, based on the user's verbal approval. A one-hour snooze is relative to selection time (6 PM → 7 PM; 6:10 PM → 7:10 PM). The label observes shared session state; it does not replace the due time or enter the editor draft. No further reference screenshot is needed for this state. Compilation/debug assembly passed; runtime and visual acceptance remain unverified.

Status: tenth increment adds the Calendar List/Day/Week/Month views, 2026-09-08. The complete milestones below are not yet marked done; Today/drawer details are tracked in [reference notes](references/README.md), Quick Add/Task Detail in [editor notes](references/EDITOR_REFERENCES.md), date/duration/reminder/repeat configuration in [schedule notes](references/SCHEDULE_REFERENCES.md), priority/list menus plus inline tag entry in [organization notes](references/ORGANIZATION_REFERENCES.md), attachment menu/cards in [attachment notes](references/ATTACHMENT_REFERENCES.md), Plan Your Day in [planning notes](references/PLANNING_REFERENCES.md), the reminder card/Snooze/Change Date in [reminder notes](references/REMINDER_REFERENCES.md), and the Calendar views in [calendar notes](references/CALENDAR_REFERENCES.md). Current verification is compilation/build only at the user's request. The broader testing proposals below are deferred under that instruction.

Read alongside [the architecture](ARCHITECTURE.md) and [the product brief](personal_task_habit_app_project_details.md).

Current user scope override (2026-09-06): defer Templates, Quick Add Settings and Suggested Tasks. Plan Your Day is implemented from the supplied reference, including horizontal card browsing and all five footer actions. The wider inventory below is a roadmap, not an instruction to implement deferred features or execute runtime/tests.

Home follow-up implemented: Overdue / Today / Upcoming tabs now filter the same repository by due date before / equal to / after today. The selected tab survives configuration changes; each has a specific empty state and shares task-row completion, detail and schedule editing. All future dates qualify for Upcoming; undated tasks stay in their lists. Compilation/debug APK assembly passed; no runtime/tests were run.

Seventh/eighth reference increments (2026-09-06): **the reminder card, Snooze options, Custom Snooze and Change Date** are implemented from the supplied 18:21 and 18:40 screenshots. A session queue and editor schedule/status reconciliation support interruption. The card's Focus action is excluded at the user's request. The ninth increment adds snoozed Task Detail from verbal approval. See [reminder reference notes](references/REMINDER_REFERENCES.md) for current scope and debug entry. UI-3 still has remaining roadmap items and no runtime/visual acceptance is claimed.

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
| Task overflow, 7 | View (List/Kanban), Background, Show Details, Show Completed, Group & Sort; Select pending | Referenced controls update the session UI. Task Manage Section, View Options and Share are excluded by the user's September 8 correction |
| Quick Add, 8 | Sheet above keyboard, title/description, date, priority, tag, list, more, voice, send | Draft survives pickers and full-screen promotion; only one task is created |
| Quick Add More, 8 | Image, Template, Convert to Note, Full-Screen, Settings | Image chooser and preview; select/apply a local template; note mode; same draft full screen; task-entry settings sheet |
| NLP, 9–11 | Live date/time and recurrence spans/chips, tags, `~` list trigger, priority menu | Exact examples from brief; chip suppression; cursor/IME intact; manual selection precedence |
| Task Detail, 12–14 | List, priority, more, date/reminder/snooze, title, description, checklist, tags, attachments | Existing edit and new full-screen creation share content; checklist edit/check/reorder works |
| Attachments, 14 | Take/Choose Photo, Records, File, Scan Documents; preview/remove/missing-file state | Camera/document results, loading, cancellation and failure UI; no recording/capture/device actions are run during current compile-only verification |
| Date/time, 15–16 | Date and Duration tabs, calendar, start/end values, time clock and keyboard input, AM/PM, Clear | Valid selections round-trip; Cancel restores prior values; invalid intervals cannot Apply |
| Reminders, 17–18 | None, on time, offsets, custom days/hours/minutes, constant reminder | Labels and validation reflect chosen values; expired trigger warning; configuration persists in session |
| Repeat, 19 | None, daily/weekly/monthly/yearly/weekdays; custom basis, interval, weekdays, specific dates | Every supported rule can be edited/reopened without data loss; conflicting choices cannot coexist |
| Reminder and snooze, 20–22 | App-wide bottom card, date/list/title/description, Close, Complete; every snooze option | Simulate delivery from any tab; complete updates task views; snooze state appears in Task Detail; multiple alerts queue |
| Suggested Tasks, 24 | Deferred by the user on 2026-09-06 | No current entry or implementation work required |
| Plan Your Day, 25 | One task at a time, progress, Done/Today/Later/Won't Do/Delete, finish/empty state | Each action advances once and updates underlying task state |
| Lists and tags, 26–27 | Create/edit, name/color, list view selection, validation, management, tag filtering | New choices appear in drawer/pickers; deleting a list moves tasks to Inbox after confirmation; deleting a tag removes associations |
| Habits home, 28, 33 | Weekly strip, selected date, sections, rows, empty state, add; section add/reorder | Date/section changes and check-ins update the same habit records |
| Habit basics, 29 | Name, icon, letter avatar, quote/refresh, Next; optional preset gallery | Required custom creation works; seeded quotes stay offline; Back preserves wizard input |
| Habit setup, 30–35 | Daily weekdays, weekly target, interval; binary/amount goal; goal days; start date; section; multiple reminders; auto-pop-up | Invalid targets/dates are blocked; saved habit reopens with identical settings |
| Habit check-in, 36–38 | Full-screen background/illustration/title/quote/control, back/more, achieved state, confetti, stats | Check-in/undo affects selected date and stats; repeat taps do not double count; reduced-motion path |
| Calendar, 4 | List/Day/Week/Month views combining tasks, habits and labeled holidays; view selector, Today/prev/next, legend | Implemented 2026-09-08 over shared repositories; occurrence-date task editing and habit check-in; recurrence expansion deferred |
| Focus, 4 | Minimal local duration selection, start/pause/reset and finished UI | Foreground session state works; background timing/recovery is deferred and explicitly scoped |
| Settings, 39–46 | Tab Bar, Appearance, Date & Time, Sounds & Notifications, Widgets, General, Integrations & Import, Backup | Controls affect the demo where applicable; platform-dependent states are reachable through fixtures; export/restore includes preview, cancel, progress, success and errors |

## 3. Scope decisions and missing references

- Today, drawer, Inbox, empty/NLP Quick Add, its More menu, Task Detail, date/duration/time/reminder/repeat screens, priority/list menus, inline tag entry, selected-list state and attachment menu screenshots have been provided and copied into `docs/references/`. The user confirmed that the tag icon inserts `#` for label entry and the Quick Add list icon inserts `~` and opens the selector. Added-attachment screenshots are unavailable; simple cards with delete are authorized. Plan Your Day is also supplied and implemented with swipeable cards. Templates, Quick Add Settings and Suggested Tasks are deferred by the user; their references are not required now. Tag management, full recording/scanning and later task/habit journeys still need detailed references. Record provisional dimensions and behavior; do not mark visual parity complete without comparisons.
- September 8 View references supersede the earlier list-only scope: List and Kanban use the same task groups. Timeline is not requested.
- Calendar received its four scheduling views in the 2026-09-08 increment (see [calendar notes](references/CALENDAR_REFERENCES.md)); statistics and production background focus timers remain outside this UI pass, and Focus keeps its small local surface.
- Habit presets are optional; custom creation is required. Templates and Quick Add Settings are currently deferred; earlier basic dialogs are retained but hidden from the menu.
- `Records` in the attachment menu is provisionally interpreted as audio recordings; it now imports existing audio. The attachment increment wires external-camera capture and document selection. Scan Documents offers a document photo or existing PDF/image; in-app recording and automatic cropping/scanning remain later work. Document scanning must ultimately work offline; a remote OCR requirement must not be introduced.
- Task View Options, Share and Manage Section are excluded. Group & Sort follows the six grouping and six sorting choices in the supplied reference, with sort direction. Background supports None, Color, Gradient and Image. Show Details controls row metadata. Task Select remains future work; Habit section management is unaffected.
- Task Detail More provisionally includes duplicate, checklist/note conversion, move, and delete. Habit More supports edit and undo check-in. Preserve entered data during conversions; confirm any lossy conversion.
- Plan Your Day: Done completes; Today replaces footer actions with Morning 10 AM / Afternoon 1 PM / Evening 5 PM / Night 9 PM; Later replaces them with Tomorrow / 3 days later / Next week (+7 days). Selection saves/advances, while Back cancels without changing the task. These options follow the user's verbal reference; premium screenshots are unavailable and not required. Won't Do marks the current task record declined (per-occurrence skipping remains deferred with recurrence execution); Delete confirms removal. Progress is measured against a stable session candidate list and tolerates external deletion/completion.
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
- Implement complete/undo, list/tag creation and selection, counts, sorting/grouping, view preferences and bulk selection. Task section management and sharing are excluded by the user's September 8 correction.
- Build search across task title/description, with results and no-results states.

**Exit:** editing fixture records through available actions updates every affected list/count/filter without navigation resets.

### UI-2 — Task entry and shared editor

Fourth increment completed within this milestone: shared four-choice priority popup; icon/list-name/checkmark list menu; `#`/`~` toolbar insertion with cursor/focus handling; inline tag reconstruction/editing; compact Quick Add toolbar and red Yesterday label. The floating menus dismiss independently of the draft. New tasks still require Send/Save, while existing valid edits use the established debounce/Back save path. Broader UI-2 scope, including detailed template/attachment/settings layouts and voice integration, remains unfinished.

Selected-list correction completed from the 10:15 reference: selecting Work produces highlighted `~💼Work` title text and a `💼 Work` toolbar action. Subsequent selections replace the current list token, share the same list ID with Task Detail, and exclude list names from date/tag parsing. Visual parity and runtime interactions remain unverified under the compile/assembly-only requirement.

Fifth increment adds the referenced five-row attachment popup, shared image/file/audio cards with delete, external photo capture, file/photo/audio selection, document-photo/existing-scan choices, optional size metadata, import loading/save protection, and bounded thumbnail decoding. The user explicitly permits a simple added-attachment layout because that reference is unavailable. Templates, Quick Add settings, collapsed Task Detail, in-app recording and automatic scanning remain unfinished. Runtime capture and UI journeys were not executed during verification.

- Build Quick Add and full-screen Task Detail from shared editor state/content.
- Add date/duration/time, reminder, repeat, priority/tag/list pickers, checklist editing/reordering and attachment flows.
- Add live NLP, suppression behavior, new-tag flow, notes/templates and simulated voice input.
- Implement validation, dirty dismissal, save failure/retry, duplicate-save protection and editor restoration across rotation.

**Exit:** type `Bring groceries tomorrow at 10 am #shopping`, inspect NLP, dismiss its chip, set a manual date, promote to full screen, add checklist/attachment, save once, reopen and edit. Every value and cursor-sensitive interaction behaves consistently.

### UI-3 — Reminder experience and daily planning

Current partial implementation: the reminder card renders live task/list data with Snooze, Complete and Close. Focus is omitted at the user's request. Snooze has the reference two-row menu, Custom hour/minute wheels and numeric input, plus a centered Change Date variant of the shared schedule picker. The user confirmed Today Night = 9 PM today (disabled after that time), and Tomorrow = original task time tomorrow. Shared session deadlines leave due dates intact and are invalidated by schedule/status/deletion changes. Debug entry: drawer → bell → Scheduled tasks → Preview Reminder, Preview in 10 seconds, or Preview all reminders. These simulate app UI presentation only; no system alarm or background notification is produced.

Reminder slice status:

- Implemented: present a task reminder over the current tab/editor through clearly identified debug-only immediate/delayed delivery controls. Render its due date/time, priority, list, title, description, checklist, Close, Complete and Snooze from shared data and the supplied reference.
- Implemented: connect 15 min, 30 min, 1 hour, 3 hours, Tomorrow, Today Night, Next Hour, Custom and Change Date to the card, following the supplied menu order and the user's time clarification. Back from Snooze returns to the card; resolving it advances the queue.
- Implemented: retain the due date/time when snoozing, show the resolved snooze time in Task Detail from the user's September 7 verbal approval, and stage Change Date with Clear/Cancel/OK using the existing schedule components. Task Detail and the debug list read the same stored deadline.
- Implemented: shared session alert state and a stable delivery queue reconcile completion, decline, deletion, note conversion, schedule and snooze changes. Actions retain their rendered delivery ID. The editor reconciles external schedule/status changes before saving while retaining typed fields/selection. Runtime behavior remains unverified.
- Keep actual alarms, recurrence execution, background/lock-screen presentation, habit reminders and the reminder-center redesign outside this slice. Calendar, Focus, Habits and the explicitly deferred Templates, Quick Add Settings and Suggested Tasks do not expand this work.

Only compilation and debug APK assembly are permitted verification for this slice. The wider UI-3 roadmap and exit journey below remain future acceptance criteria, not authorization to launch an emulator/device or run unit/end-to-end tests.

- Build reusable reminder card, all snooze choices, occurrence queue, and local reminder center.
- Inject task and habit reminder deliveries from the scenario catalog while any tab or editor is open.
- Add permission-denied, notification-channel-disabled, exact-alarm-unavailable, and restricted-popup fixtures.
- Plan Your Day implemented in the sixth increment: horizontally swipeable cards, five shared-data actions, stable review progress, same-day resume, finish and no-candidate states. The follow-up corrects Today/Later to in-place footer choices from the user's verbal reference. Suggested Tasks is deferred by the user. See planning reference notes for the current recurrence limitation.

**Exit:** a simulated reminder can be completed, dismissed, or snoozed; updated state appears in task detail and Today. Interrupted drafts survive. All planning actions produce the specified shared-data changes.

### UI-4 — Habits end to end

Progress 2026-09-08: tapping records now opens the referenced check-in screen with Edit/Share/Archive/Delete. Binary/amount recording and Undo update shared totals and task visibility. Active/Archived, Habit Settings and section management are also implemented. Celebration, streaks, weekly/finite-goal achievement and runtime/visual validation still block the UI-4 exit gate.

Progress 2026-09-07: home, date strip, sections and custom creation/editing are implemented from the corrected screenshots. Frequency, goal, goal days, start date, reminder preferences, icons/letters and quotes save to the session repository. See [Habits references](references/HABIT_REFERENCES.md). Check-in/undo, achievement, celebration, streaks and runtime validation remain open; this does not complete the UI-4 exit gate.

- Build date strip, sections, custom habit wizard, icon/avatar/quote choices, frequency, goals, start date, reminders and edit mode.
- Build full-screen check-in, amount entry, achieved state, celebration, stats and undo.
- Implement pure schedule/check-in rules required by these interactions with focused correctness tests; leave actual alarm registration behind the simulator.
- Cover selected weekdays, weekly target, interval, finite goal, before-start and already-achieved scenarios.

**Exit:** create a habit, change selected date, check in, undo, edit it, and inspect consistent history/statistics. Ineligible dates and duplicate actions are handled deliberately.

### UI-5 — Remaining surfaces and settings

Progress 2026-09-08: the Calendar journey is implemented beyond the originally minimal scope — List/Day/Week/Month views, a labeled sample-holiday layer, and the shared completed-visibility control — validated by compilation/debug assembly only. Focus and the settings surfaces below remain open.

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
