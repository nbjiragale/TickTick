# Reminder card and Snooze reference increments — 2026-09-06

## Popup delivery repair — 2026-09-27

Fresh background reminders now attach the full-screen notification intent whenever full-screen access and the existing channel/DND gates allow it. Android chooses the lock-screen activity or an unlocked heads-up notification at presentation time, avoiding a stale lock-state decision in the app. Display-over-other-apps access still enables the custom card over an unlocked app. Full-screen PendingIntents explicitly opt into creator background-launch privileges on Android 15+, using the Android 16 mode where available. The popup activity declares show-when-locked and turn-screen-on before creation and saves pending delivery/revision requests across recreation.

Reminder settings now identify low/disabled popup channels, active Do Not Disturb and a persisted alert pause, alongside the existing permission links. The original card, Snooze and Change Date UI and shared repository actions remain in use. These are source-level repairs; real-device acceptance remains pending under the compile-only constraint. See the backend plan for validation evidence.

## Sound settings — 2026-09-25

Settings -> Sounds now offers separate Task completion, Notification, Habit completion and Reminder popup choices through Android's built-in tone picker, including System default and Silent. Values persist through the existing Room preference store. Background popup reminders use the popup channel/tone once; notification-only reminders use the notification tone. Foreground task popups use bounded in-app playback; task completion feedback follows successful shared repository commits, including recurrence, batch and notification actions. Missed summaries stay silent. Sound changes rebind the current channels without replaying alerts; Android channel overrides and DND still apply. Device audio and picker/restart behavior remain unverified; see the [backend plan](../BACKEND_IMPLEMENTATION_PLAN.md).

## Background popup correction — 2026-09-25

The user confirmed that notifications arrive without the popup. A dedicated translucent ReminderPopupActivity now reuses this card and its full Snooze/Change Date flow over another app when Display over other apps access is granted. Locked-screen delivery uses Android's full-screen notification path where permitted. Settings (also reachable from the drawer bell) exposes both special-access settings and the actual task/habit notification channels. Popups respect high-importance channels, Do Not Disturb, pause and silent missed-reminder recovery. Ordinary notification fallback remains available. Incoming alerts queue, stale targets are ignored, and the main screen only auto-presents while resumed. Debug previews remain foreground-only simulations. Source implementation and compile evidence do not establish real-device/visual parity; see the [backend plan](../BACKEND_IMPLEMENTATION_PLAN.md) for exact checks and pending acceptance.

## Snooze confirmation follow-up — 2026-09-08

Verification: final debug Kotlin compilation and APK assembly passed (`BUILD SUCCESSFUL`, 49s) with `:app:assembleDebug -Pkotlin.incremental=false -Pkotlin.compiler.execution.strategy=in-process --console=plain`. No Kotlin warnings were reported. No runtime, device, unit, timing or visual tests were run under the existing build-only constraint.

The user requests a very small confirmation card immediately after choosing a snooze, visible for two seconds. Successful preset and custom snoozes now emit a transient acknowledgement from ReminderViewModel. Duration choices say, for example, "We'll remind you in 15 minutes." Custom values format hours/minutes naturally; Tomorrow, Today Night and Next Hour display their resolved local day/time. Opening Snooze, cancelling, stale actions and failed snoozes do not produce success feedback. Change Date retains its separate schedule-edit behavior.

The white rounded card has a small blue check and concise text, with no buttons or dimmed backdrop. A compact centered window appears above open editors and the next queued reminder, with non-focusable/non-touchable flags so it does not intercept input. It remains independent of reminder queue state and editor drafts. A new successful snooze replaces the previous notice and starts a fresh two-second timeout. The ViewModel owns that timeout, so ordinary rotation does not restart it; the notice is not saved for replay after process restart. The text is exposed as a polite accessibility announcement. Actual alarm delivery remains deferred: this confirms the existing session snooze operation, not new backend integration.

Status: the reminder card, session alert queue, Snooze options, Custom Snooze and Change Date are implemented from the supplied screenshots. Task Detail's snooze status is implemented from the user's September 7 verbal approval. The user explicitly excludes the reminder card's Focus action. No runtime or visual parity claim is made.

## Supplied evidence

| Reference | Original filename | Implemented surface |
| --- | --- | --- |
| [Reminder card](reminder-popup.jpg) | `photo_2026-09-06_18-40-35.jpg` | Priority/date/list header, title, description/checklist, Close, Snooze and Complete |
| [Snooze options](snooze-menu.jpg) | `photo_2026-09-06_18-21-37.jpg` | White bottom card, Back/title, two rows of four choices, Change Date footer |
| [Custom Snooze](snooze-custom.jpg) | `photo_2026-09-06_18-21-36.jpg` | Back/title/check, wrapping hour/minute wheels, keyboard alternative, resolved time below |
| [Change Date](snooze-change-date.jpg) | `photo_2026-09-06_18-21-35.jpg` | Centered Date/Duration dialog above Snooze, calendar, Time/Reminder/Repeat, Clear/Cancel/OK |

The four 576 × 1280 reference copies are preserved unchanged under `docs/references/`; originals remain in Downloads. `photo_2026-09-06_11-18-25.jpg` is the existing [Plan Your Day reference](PLANNING_REFERENCES.md); no planning behavior changes were requested by resending it. The additional `photo_2026-09-06_18-38-45.jpg` repeats the Snooze menu and does not need a second baseline copy.

The browser header, ChatGPT sign-in error and system UI behind Snooze are screenshot context, not app content or instructions. No browser, sign-in, cloud account, premium restriction or subscription UI is introduced.

## User-confirmed time choices

The screenshot shows **Today Night**, replacing the earlier brief's Tomorrow Morning option in this menu. The user explicitly confirmed:

- Today Night snoozes until **9 PM today** and is disabled at/after 9 PM.
- Tomorrow returns at **the task's original due time tomorrow**.

For date-only tasks, Tomorrow uses their configured `ReminderConfig.dateOnlyTime` (currently 9 AM by default). This fallback was stated during implementation. Next Hour resolves to the next whole hour; duration choices are relative to the current instant. These latter rules are implementation decisions consistent with the existing plan, not behavior established by a still image.

## Implemented interaction

| Control | Result |
| --- | --- |
| Reminder Close / system Back / outside tap | Dismiss only the current session alert, leave the task active, and reveal the next queued alert |
| Reminder Complete | Complete the current task in the shared repository and reveal the next queued alert |
| Reminder Snooze | Replace the card with the existing Snooze options in the same dialog |
| 15 mins / 30 mins / 1 hour / 3 hours | Store a separate snooze deadline relative to now and close Snooze |
| Tomorrow | Store tomorrow at the original due time, using the date-only fallback above |
| Today Night | Store today at 21:00; unavailable when the time has passed |
| Next Hour | Store the next whole-hour instant |
| Custom | Replace the options with the captured hour/minute picker; initially 0 hours / 30 minutes |
| Custom wheels | Select 0–23 hours and 0–59 minutes with wrapping, snapping lists; surrounding values fade |
| Custom keyboard icon | Switch to bounded numeric fields; the clock icon switches back to wheels |
| Custom check | Apply a positive duration; zero cannot be submitted, and wheel movement disables submission until settled |
| Custom footer | Show the resolved local day/time; labels derive from the injected clock |
| Change Date | Open the shared schedule editor using its new centered presentation |
| Change Date Clear | Clear the staged schedule; OK commits, Cancel preserves the task and snooze |
| Change Date Time/Reminder X | Clear that staged field without opening its picker |
| Change Date OK | Validate/apply the schedule, clear the previous snooze, and exit the Snooze flow |
| Back | Custom/Change Date return to Snooze options without saving; Back from options returns to the reminder card |
| Outside dismissal | Cancel uncommitted Snooze input and return to the reminder card; outside Change Date cancels that child dialog |

The keyboard fields reuse the existing app's numeric input style. Their expanded screenshot is unavailable, so that alternative input layout remains provisional within the supplied Custom screen. Task Detail's snooze presentation is not inferred from these screenshots. The circular header action in the reminder reference is Focus; the user confirmed its meaning and requested no implementation, so that control is omitted.

## Geometry and shared controls

The reminder card uses the same 10 dp outer margins and 14 dp rounded white container as Snooze. At the reference phone width it is 273 dp high, with 20 dp content side/bottom padding, a priority flag and 14 sp blue due label, muted list metadata, a top-right Close action, a 20 sp bold title, and 15 sp description/checklist text. Long text scrolls inside the content area while the two bottom actions remain reachable. Snooze has a light gray fill; Complete has a pale blue fill; both have 8 dp corners and a 42 dp minimum height. Titles, priority, date/time, list and checklist rows use live task data; screenshot examples are not hardcoded. Checklist items appear as read-only dash-prefixed lines, with completed items struck through.

Retain the project's provisional mapping of 576 screenshot pixels to approximately 384 dp. The Snooze card has 10 dp outer margins, 14 dp corners, a 56 dp header, two minimum 80 dp option rows, and a centered Change Date action. Custom uses the same container with 160 dp wheels, 32 dp wheel rows, a bottom keyboard control and a centered 14 sp resolved-time label. Options use 32 dp blue outline icons, 12 sp labels and 14 sp duration badges. The header is 16 sp bold. Small screens and larger fonts can scroll the card content.

Change Date reuses `SchedulePicker`, `MonthCalendar`, `ScheduleOption`, reminder/repeat controls, the clock/keyboard time picker and duration controls. Its centered variant has 29 dp phone side margins, 20 dp corners, a gray background, header tabs and bottom Clear/Cancel/OK controls. Time and Reminder have clear actions; values use muted gray as in the reference. The existing Quick Add/Task Detail schedule sheet retains its established presentation and Clear behavior.

System insets remain native. Exact density, OEM font, font scaling, dialog geometry at runtime and pixel parity remain unverified.

## State and entry point

`feature/reminders/ReminderViewModel` now owns the reminder/Snooze flow, replacing `SnoozeViewModel`. `ReminderHost` replaces `SnoozeHost` and is composed after the editor and planning hosts so its dialog can appear above them. `ReminderCard` receives task/list values and callbacks. The ViewModel observes the existing application-scoped `TaskRepository`; it never opens or replaces the editor draft to deliver an alert.

Each queued delivery has a stable ID, task ID, and captured schedule/snooze identity. Task content stays live in the repository. Duplicate pending deliveries for the same task are ignored. Completion, decline, deletion, note conversion, schedule edits or snoozing reconcile obsolete entries out of the queue. Resolving an alert advances once, and actions retain the rendered delivery ID so a stale or repeated callback cannot act on the next task. New cards reset their content scroll position. The queue and pending debug deliveries survive ordinary configuration changes through the app-scoped ViewModel but reset on process restart; they are not reconstructed from restarted fixtures. Small custom-input values use `SavedStateHandle`.

`TaskSnapshot.snoozedUntil` stores session deadlines by task ID, separate from task due dates and editor drafts. Ordinary task edits retain the deadline; schedule changes, completion, decline, note conversion or deletion invalidate it. `TaskRepository.snooze` only changes the deadline, and `changeSchedule` applies schedule fields to the latest task atomically without replacing its title, description or other content. Actions recheck the target's eligibility. This is session UI state, not an alarm queue or durable storage.

To reach the implemented screens in the debug APK: **drawer → bell/notification shortcut → Scheduled tasks → Preview Reminder**. This replaces the earlier Preview Snooze entry. The debug list identifies the flow as a UI demo with no system alarms. **Preview in 10 seconds** closes the list and queues a foreground presentation after a ViewModel delay, allowing another tab, Quick Add, Task Detail or planning to be opened first. **Preview all reminders** queues eligible listed tasks, presented one at a time. Returning to Scheduled tasks after snoozing shows the stored deadline. These controls are absent from the release source-set binding; none were exercised during compile-only verification.

`TaskEditorViewModel` observes external schedule/status changes and reconciles them again immediately before saving. A reminder's completion/date change takes precedence for those fields while retaining typed title, description, checklist/attachment edits and text selection. A stale open schedule picker closes when the underlying schedule changes. This prevents the editor's existing full-record save from restoring an outdated status/schedule after a reminder action. Snooze deadlines remain separate from editor serialization. New drafts and unrelated tasks are not replaced by reminder presentation. Runtime window/keyboard restoration remains unverified.

Actual alarms, notifications/background/lock-screen delivery, Constant Reminder re-alerts, recurrence advancement, Room and process-restart recovery remain deferred. Stored deadlines alone do not schedule delivery.

## Task Detail snooze status — verbal reference, 2026-09-07

The user confirmed that choosing a one-hour snooze at 6 PM should produce a 7 PM snooze deadline. They approved displaying "Snoozed until 7 PM" while preserving the original due time separately, with the interval starting when Snooze is selected (6:10 PM → 7:10 PM), then explicitly requested implementation. No further screenshot is required for this increment. The existing reminder-popup and Snooze screenshots are also supplied and should not be requested again.

Task Detail keeps the original due-date/time row and adds a blue Snooze icon with a smaller wrapping "Snoozed until Today 7:00PM"-style line directly below it. Today/Tomorrow/date and time formatting reuse the Custom Snooze/debug-list formatter. The line is read-only; the existing due row still opens the schedule picker. This uses the current editor's styling under verbal approval and makes no pixel-parity claim.

The deadline comes directly from the shared snapshot, so the label updates during an open Task Detail session and on reopening. It does not become a draft field or replace the original schedule. Active existing non-note tasks show it while a deadline is stored. Completion, decline, note conversion, deletion and committed schedule changes retain the repository's existing invalidation behavior; ordinary content edits preserve it. Reaching the stored deadline does not yet deliver another alert: actual scheduling remains deferred.

The session queue reuses the same card one at a time; it introduces no unreferenced multiple-alert layout. Focus on the reminder card is explicitly out of scope. Actual alarm delivery and all earlier deferred features remain deferred.

## Verification

Backend update (2026-09-21): B3 routes notification/card Complete through the shared occurrence transaction, recording history, advancing once, clearing snooze and reconciling the next due alarm. Occurrence/revision checks reject obsolete actions; schema-2 dismissed state is preserved when adopting recurrence identities. B2 cadence and silent recovery remain in effect. See the [backend plan](../BACKEND_IMPLEMENTATION_PLAN.md). Real recurring delivery remains unverified; earlier deferred-recurrence statements are superseded.

Only Kotlin compilation and `:app:assembleDebug --console=plain` are permitted. No emulator, device, unit, end-to-end or runtime UI tests are run, and build success does not certify visual parity.

Final compilation and assembly passed after the Task Detail snooze-status increment on 2026-09-07: `BUILD SUCCESSFUL in 1m 15s`, 4 tasks executed and 33 up-to-date, with no Kotlin warnings. Command uses `JAVA_HOME=C:\Program Files\Java\jdk-17`. APK: `E:\TickTick\app\build\outputs\apk\debug\app-debug.apk`.


## Backend connection — 2026-09-18

B2 connects the existing card/Snooze/Change Date flow to durable deliveries and shared transaction-backed actions. Background notifications provide Done, Snooze 15 min, Change Date and swipe-dismiss. Notification taps validate delivery revision; stale targets show feedback. Snooze deadlines now schedule real alarms without replacing due time. Debug previews remain explicit, but snooze/task edits from previews now update the real backend. Persisted dismissal stops constant re-alerts; at most four follow-ups use 15-minute spacing. Late recovery is silent and consolidated. Alert pause/resume and notification/exact-alarm capability controls are connected. See [backend plan](../BACKEND_IMPLEMENTATION_PLAN.md) for migration/recovery details. This supersedes the earlier deferred-alarm paragraphs; recurrence advancement, habit alarms and all device/visual acceptance remain pending.
