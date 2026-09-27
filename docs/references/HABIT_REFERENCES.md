# Habits home and custom creation — 2026-09-07

## Sound settings — 2026-09-25

The main Settings page now exposes Habit completion separately from Notification and Reminder popup. The old habit ringtone row edits the shared Notification choice, avoiding two conflicting selectors; its prior saved tone is the fallback until the new choice is saved. Native Android tone selection includes System default and Silent. Completion audio plays only after committed progress crosses the dated target, including notification check-in; partial progress, Undo and stale/failed actions stay quiet. Foreground automatic habit log opening plays the Popup tone once. Background popup audio comes from its notification channel. Runtime audio, native picker and restart behavior remain unverified; see the [backend plan](../BACKEND_IMPLEMENTATION_PLAN.md).

## Background popup correction — 2026-09-25

Auto pop-up of habit log now also uses the existing check-in UI in the dedicated reminder window outside MainActivity, subject to the app's Display over other apps permission or Android's lock-screen full-screen alert access. The per-habit Auto pop-up switch is still required; it is not silently enabled. Popup check-ins retain automatic/manual quantity behavior and shared revision-checked writes; management actions remain in the ordinary habit screen. The main screen no longer auto-opens/acknowledges a habit while paused. Settings exposes habit channel importance and popup permissions. See the [backend plan](../BACKEND_IMPLEMENTATION_PLAN.md); device/visual behavior remains unverified.

## Backend B5 draft update — 2026-09-25

Habit editor configuration and step now persist as a versioned Room draft using the same habit codec as storage. Recovery preserves the original habit revision; stale or deleted owners fail save without overwriting records. Applied child choices are retained, and cancelled/unapplied child choices stay outside the draft. Successful habit save clears the acknowledged draft in the habit transaction; closing/discarding queues a tombstone. Storage failures remain retryable, and invalid drafts are retained with a recovery/discard message. Habit home receives the shell's date refresh on resume/midnight. Existing B4 settings persistence is reused. See the [backend plan](../BACKEND_IMPLEMENTATION_PLAN.md) for build evidence; process-death and device/visual behavior remain unverified.

## Backend B4 update — 2026-09-24

This update supersedes the session-only storage and deferred rules/reminder statements below. The supplied layouts and the latest List placement override remain the UI contract; no new visual parity is claimed.

Habits, sections/order, archive state, settings and dated quantities now use Room schema 4 through the shared application repository. Completed dates are derived from quantity and the target effective on that date. Production starts empty in both build variants; fixture helpers remain demo-only. Save, check-in, Undo and section creation await database acknowledgement. Failed editor/manual-amount/section-add writes retain input; stale habit revisions prevent overwriting newer progress.

Daily and interval habits count scheduled opportunities. Weekly habits accept any eligible day until the Monday-based weekly target is met; a partial week requires at most its unarchived eligible days. Finite goals retire later opportunities after the required completed-day count. Undo and backdated entries recompute completion, goals and streaks. Today's unfinished opportunity and the current unfinished week remain pending. The existing compact log shows current/best streaks; switching between week and day units starts a new streak era. Archived periods preserve history and do not break streaks. These are backend policy interpretations, not new screenshot-derived statistics or celebration designs.

Schedule, amount, unit and goal edits preserve historical configurations and take effect tomorrow, or next Monday for changes involving weekly frequency. The editor states the effective date. Metadata, recording preferences and reminder-time edits apply immediately. The check-in screen displays the target/unit effective on its selected date.

Multiple reminder times use the B2 alarm/recovery pipeline and persisted delivery identities. Completion/archive/achievement cancel obsolete delivery; restoring eligibility can restore future reminders without replaying old dismissed ones. Constant Reminder shares B2's bounded cadence (15 minutes, at most four follow-ups after the initial alert). Opening, dismissal or a check-in action ends the active repeat cycle. Auto pop-up opens the existing habit detail only while the app is in the foreground; background notifications provide Open habit, Dismiss and binary/automatic Check in. Manual quantities use the existing amount entry. Missed alerts use a silent summary; catch-up is limited to today.

Selected sound/Silent and badge preference are connected to Android notification channels. Android overrides and launcher capabilities still apply. TickTick Pop has no bundled asset, so its default uses the system notification sound; settings states this limitation. No sound, alarm, badge, migration, restart, gesture or visual behavior has been verified on a device. Validation remains compilation/debug APK assembly and static schema/source inspection only. The [backend plan](../BACKEND_IMPLEMENTATION_PLAN.md) records final build evidence and the next B5 work.

## Latest placement override — List view, 2026-09-08

The user subsequently requested that List layout show habits alongside tasks, while Kanban remains as implemented. The home List view now includes the separate Habit card from shared records: today's habits under Today/Overdue and the next seven days under Upcoming, respecting Show Completed and the existing Show in Today & Next 7 days preference. Archived/unscheduled records stay excluded. Habit-only content also renders when tasks are empty. Check-in opens for the selected occurrence and returns to the task list. This supersedes the task-only List behavior in the earlier placement correction below. Calendar remains a combined view; Inbox/custom lists do not acquire unrelated habits. See [task-view notes](TASK_VIEW_REFERENCES.md).

## Tasks / Calendar placement correction — 2026-09-08

The user clarified the bottom-tab content: the first Tasks tab contains tasks and its Add action only; Calendar (second tab) is the combined task-and-habit view. Habit rows and all habit input/callback parameters have been removed from `TasksScreen`, including Today and Upcoming. `CalendarContent` now observes shared task and habit records for its selected date, with Tasks and Habits sections and a combined empty state. Habit rows open check-in for that date; archive, schedule and Show Completed filtering remain consistent with shared state. Calendar's date is retained across tab switches.

This supersedes the earlier Today/Upcoming placement described below. The reference's Show in Today & Next 7 days preference remains stored, but cannot inject habits into the first tab; the combined Calendar day view shows both record types independently of that preference. No Calendar screenshot or new calendar-view design was inferred. Debug build verification passed; runtime/visual checks remain pending.

## Habit check-in and overflow menu — 2026-09-08

[Habit check-in](habit-check-in.jpg), `photo_2026-09-08_12-51-45.jpg`, and [its overflow menu](habit-actions.jpg), `photo_2026-09-08_12-51-47.jpg`, now define what tapping a habit opens. Home preserves the selected day; Today/Upcoming carries the occurrence's day; the Active/Archived library opens today's detail. Editing is available from the overflow menu (and the library's existing long-press action).

The full-screen view uses the blue reference background, white navigation, a locally drawn vector rendition of the toy blocks and their shadow, a centered title/quote, and the rounded check-in track. The four menu rows are Edit, Share, Archive (Unarchive for archived records) and Delete. Edit returns to this detail when saved/cancelled. Share opens Android's text chooser and leaves recipient/send choices to the user. Archive retains history and returns to the caller; Delete confirms before removing both the record and its history.

The completion control supports dragging right or tapping, with an accessible check-in action. Binary habits check in once per date. Amount habits add the configured automatic amount or open a manual total entry; completion occurs when the daily target is reached. Counts, home sorting and completed-task-list visibility observe the shared snapshot. Future, archived and unscheduled dates cannot receive new progress. The lower chevron opens a compact log summary and Undo; the completed control shows its knob on the right. These log/amount/completed presentations are functional continuations, not claims of matching an unsupplied celebration reference.

`HabitSnapshot.progress` stores per-date quantities alongside completed dates. Repository updates keep them synchronized, and repeated binary completion does not duplicate the date. Undo removes that date's progress/completion. The debug Daily Check-in starts yesterday with one completed day and the pictured quote, so today opens with an actionable control and a total of one. The supplied block illustration is used across habits pending further visual references.

Validation: debug Kotlin compilation and APK assembly passed; no device, emulator, unit or end-to-end tests were run under the build-only constraint. Runtime gestures/chooser behavior and pixel parity are unverified. Streaks, weekly-target achievement, finite-goal retirement, celebrations, durable storage and Android reminder delivery remain later work.

## Active/Archived and Habit Settings — 2026-09-08

The second header icon now opens the full-screen [Active/Archived list](habit-active-archived.jpg), from `photo_2026-09-08_12-32-35.jpg`. The third opens [Habit Settings](habit-settings.jpg), from `photo_2026-09-08_12-32-34.jpg`. Settings → Manage Section uses the full-screen [section reference](habit-manage-section.jpg), from `photo_2026-09-08_12-32-33.jpg`. These replace the earlier provisional All habits and Manage Section dialogs.

- Active and Archived have separate tabs, rounded habit cards, total completed-day counts, a back action and Add. Tap a card to edit; long-press for Edit and Archive/Unarchive. Accessible archive actions are available without long-press. Archiving preserves the record and history, excludes it from home/task lists, and restoring makes it eligible again. Tab selection survives opening an editor or managing its sections.
- Habit Settings defaults match the reference: TickTick Pop, sorting off, Show in Today & Next 7 days on, app badge off. Changes update the shared session settings immediately. Sort by Check-in Status moves unchecked habits above checked habits within each home section.
- Show in Today & Next 7 days controls habit occurrences displayed below tasks in Today and the next seven dates of Upcoming. Overdue and individual task lists do not receive habits. Archived habits are excluded, completed occurrences follow Show Completed, and selecting an occurrence opens the habit editor.
- Habit Ringtone opens Android's notification-sound picker and stores the selected title/URI or Silent; cancelling retains the previous preference. TickTick Pop remains the reference default label; the source app's sound asset is not bundled, and the platform picker starts from the system notification default until a sound is chosen. Notification playback and launcher app-badge delivery remain deferred with the notification engine; the badge preference is saved without claiming OS badge support.
- Manage Section has the four default sections, drag handles and Add Section in one white card. Drag a handle to reorder, or use accessible Move up/Move down actions. Add opens a name dialog and rejects blank/duplicate names. The editor's section shortcut uses this same page, with Back returning to the correct caller.

Habit records, archive state and settings remain session-only. The first statistics shortcut is unchanged; no detailed statistics screen is inferred from these references. Verification remains compilation/debug APK assembly only, without emulator/device or visual parity claims.

## Multiple reminders and Constant Reminder — 2026-09-08

The follow-up [multiple-reminder screenshot](habit-multiple-reminders.jpg), `photo_2026-09-08_12-22-07.jpg`, replaces the initial vertical reminder rows. Times now use 24-hour `HH:mm` labels on pale rounded chips, with four fitting the reference phone width and fewer on narrower screens. The inline `＋ Add` action wraps with the chips. Tap a chip to edit; Clear in its time picker removes it. Cancel preserves the original time. Times remain sorted and deduplicated.

When reminder times exist, a divider separates the chips from the Constant Reminder label, decorative crown and switch. The crown adds no premium restriction. `Habit.constantReminder` is saved with the draft and shared habit record independently of Auto pop-up. Removing the final reminder clears the preference and hides the row, matching the earlier empty-reminder reference. Repository validation prevents enabling the preference without a reminder time.

The user's supplied meaning is retained: repeated reminders continue until the user takes action. This is a stored setting in the current UI phase; actual repeated alert delivery remains part of the later reminder engine. Task reminder configuration already has a constant flag. No countdown feature or alert interval is introduced by this layout correction.

Validation: debug compilation and APK assembly only; device/visual checks remain unverified.

The corrected September 7 references supersede the September 6 attachments for this increment. Earlier images remain references for task editing and reminders.

| Screenshot suffix (19:40) | Saved reference | Surface |
| --- | --- | --- |
| 21 | [habits-home.jpg](habits-home.jpg) | Header, seven-day strip, Others card and add button |
| 31 | [habit-details.jpg](habit-details.jpg) | Name, icon/letter avatar, quote and Next |
| 30 | [habit-daily.jpg](habit-daily.jpg) | Weekday selection and configuration cards |
| 29 | [habit-weekly.jpg](habit-weekly.jpg) | Weekly frequency wheel |
| 26 | [habit-interval.jpg](habit-interval.jpg) | Interval frequency wheel |
| 27 | [habit-goal.jpg](habit-goal.jpg) | Binary goal selector |
| 28 | [habit-amount-goal.jpg](habit-amount-goal.jpg) | Quantity, unit and recording preferences |
| 25 | [habit-goal-days.jpg](habit-goal-days.jpg) | Goal-day presets |
| 23 | [habit-goal-days-custom.jpg](habit-goal-days-custom.jpg) | Custom 1–999 days |
| 24 | [habit-start-date.jpg](habit-start-date.jpg) | Start-date calendar |
| 22 | [habit-time.jpg](habit-time.jpg) | Reminder time picker |

## Implemented

- Habit home has a seven-day strip ending on today, collapsible section cards, total completed-day labels and an add button. Swipe to move a week; accessibility actions also expose previous week, next week and Today. Tap a date to filter scheduled habits.
- Add opens the two-step custom editor. Tap a habit row, or find it in All habits, to edit. Name is required; icon, letter avatar, quote editing and quote refresh are available. Back from settings retains the details; closing details cancels. Child dialogs stage their values until confirmation.
- Daily keeps at least one weekday selected. Weekly accepts 1–7 days per week; Interval accepts every 1–30 days. Wheels support scrolling, tapping and accessibility actions. Weekly habits are available on all dates after their start so any day can contribute to the eventual target.
- Goals support binary or positive quantity, units, Auto/Manual recording preference and automatic record amount. Goal Days supports Forever, 7, 21, 30, 100, 365 or custom 1–999. Start Date uses the calendar; reminders reuse the dial/keyboard time picker. A new habit reminder opens at the current local time; editing one opens at its saved time. Multiple times are sorted, deduplicated, editable and removable.
- Default sections are Morning, Afternoon, Night and Others. Sections can be added and reordered; these shared edits apply independently of the habit draft. Others is visible when settings opens.
- Save updates the shared session repository and selects the first eligible date on or after today/start date. The notebook shortcut lists all habits, including those not scheduled on the selected date. The statistics shortcut shows existing habit/day totals.
- Debug includes the pictured Daily Check-in in Others with one completed day. Release starts empty. Newly created habits have zero completed days.

## Visual and scope limits

The 576 × 1280 images use the existing approximate 1.5 px/dp scale: pale app background, 14 dp white card corners, 16 dp editor margins, 16 sp body labels, 20 sp headings, 44 dp bottom action and the existing 326 dp modal family. Interval/weekly cards are taller than Daily. The form scrolls while the bottom action remains visible.

Emoji on colored circles approximate the illustrated icon palette; original icon assets are unavailable. The user's September 8 correction establishes Habits as the third tab with the pin-shaped reference icon. Navigation is Tasks, Calendar, Habits, Timer; the timer has a separate round clock icon and retains its existing session controls. Empty state, letter entry, section management, toolbar summaries and unit/record menus are functional interpretations where screenshots show only entry controls. Unit choices are Count, Minute, Hour, Page, ml and km.

Home, creation/editing and the subsequent check-in/menu flow above are implemented. Celebrations, streaks, goal achievement and detailed statistics remain later work. Reminder and auto-pop-up preferences are stored but do not register alarms or open logs. Habit/section/history records are in memory and reset after process restart. The serializable editor draft and small UI values use saveable state for configuration restoration.

## Verification

Debug Kotlin compilation and APK assembly passed. No emulator, device, unit or end-to-end tests were run, following the project's build-only constraint. Runtime behavior and visual parity remain unverified. APK: `app/build/outputs/apk/debug/app-debug.apk`.
