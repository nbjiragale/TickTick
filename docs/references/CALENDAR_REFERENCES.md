# Calendar reference notes

## Backend B5 update — 2026-09-25

Calendar mode, selected date and completed visibility now persist in Room. Completed visibility is independent of task destinations. Today follows the current local date across midnight/resume; browsing a different date keeps that selection. Date/zone refresh comes from the shared task shell, while task/history/habit records remain the existing shared sources. B3 projections and B4 habit rules are preserved. Debug sample holidays remain labeled; release is empty until a source/region is chosen. Preference write failures are explicit. See the [backend plan](../BACKEND_IMPLEMENTATION_PLAN.md) for build/static schema evidence; restart, midnight/zone and visual behavior remain unverified. These notes supersede the historical saved-state-only and shared completed-toggle descriptions below.

## Backend update — 2026-09-21

B3 supplies shared due-date/specific-date projections and stored occurrence history (with Show Completed). Completion-based futures remain unknown. Projected/history checkboxes are disabled with explanatory accessibility text; opening an entry edits its current series. Current occurrence actions remain enabled. Stable keys distinguish overlapping durations/history, and each per-date projection is capped at 512 occurrences. See the [backend plan](../BACKEND_IMPLEMENTATION_PLAN.md). This supersedes the earlier current-date-only recurrence status; device and visual checks remain pending.

Status 2026-09-08: no calendar screenshots have been supplied. The user authorized reasonable design decisions without repeated confirmations, so this increment is implemented from the product brief, the existing light visual style, and the decisions recorded here. Visual parity claims are not made; a later reference pass can adjust geometry and colors.

## Scope

The second bottom-navigation tab now hosts `feature/calendar` with four views over the shared session repositories: List, Day, Week, and Month. The earlier provisional `CalendarContent` in `app/SecondaryTabs.kt` was removed. The first tab remains task-only; the third tab remains the dedicated Habits feature.

## Shared behavior

- View selector: four text segments (List/Day/Week/Month) with `Role.Tab` semantics and a selected pale-blue pill. The selected view and selected date are saveable state inside the tab's `SaveableStateProvider`, surviving tab switches, rotation, and process recreation.
- Navigation: previous/next move by day (Day), week (Week), or month (List/Month, clamping the day-of-month at short months). Today re-selects the current date. The period label shows "September 2026", "EEE, MMM d", or a week range, omitting the current year.
- Occurrences: tasks appear on `dueDate`, or on every date covered by their `TaskDuration`. Single-day timed durations occupy `startTime..endTime`; multi-day timed durations render a start segment, all-day middles, and an end segment; `dueTime`-only tasks get a default one-hour block; date-only and all-day items sit in the all-day area. Habits use `habitsFor(date)` (schedule, archive, check-in sorting) and render as all-day entries — a reminder time appears as a label only. Holidays are all-day and display-only.
- Completed visibility: the calendar respects the shared Show Completed setting (also toggleable from the calendar's overflow menu). Hidden by default; when shown, completed/declined tasks and checked-in habits are struck through and muted.
- Interactions: task blocks open the shared Task Detail editor and list rows expose the completion checkbox; habit blocks open the check-in screen carrying the tapped occurrence date through the existing Habits-tab routing; holiday blocks are not interactive.
- Repeat rules are not expanded into occurrences; a repeating task appears only on its current due date, consistent with the deferred recurrence engine.

## Category styling

Soft light blocks on the existing pale background, with icons/labels so color is never the only distinction, plus a legend row (colored chip + icon + name):

- Tasks: container `#DFE9FD`, ink `#2D5FC9`, border `#C3D6F8`, check-square glyph and checkbox affordance.
- Habits: container `#DDF2E4`, ink `#2E7D4F`, border `#BFE4CC`, the habit's emoji avatar/glyph.
- Holidays: container `#FBEED6`, ink `#96660F`, border `#EDD9AE`, flag glyph and a "Holiday" label.

Titles use the standard text color with two-line ellipsis in rows, tighter truncation in Week/Month chips.

## Per-view notes

- List: agenda for the selected date's month grouped by date headers ("EEE, MMM d · Today"); empty dates omitted; an empty-month state; initial scroll to the selected date.
- Day: stacked all-day chips (holidays, date-only tasks, habits; capped height, scrollable) above a 24-hour timeline with an hour gutter, overlap clusters sharing width, a red current-time line on today, and initial scroll near the first event (else ~8 AM).
- Week: Sunday-start, matching the schedule picker's month calendar. Day headers select the date; each column shows up to two tiny all-day chips plus "+n" (tapping "+n" selects the day); the same timeline renders seven compact columns.
- Month: Sunday-start grid sized to the month's real week count; adjacent-month days are muted but tappable (moving the anchor month); cells show the day number (today filled blue, selected pale-blue cell) with up to two mini chips and "+n"; the selected day's agenda renders below using the List rows.

## Holidays

The user skipped the holiday-source question, so no region is assumed and no real-world holiday dates are invented. `HolidayRepository` (read-only, in-memory) is wired in the app container; the debug seed provides four holidays explicitly labeled "(Sample)" relative to the current clock under the source label "Sample holidays", which the legend displays. The release seed is empty ("No holiday source configured"), and the legend omits the holiday chip when no holidays exist. Supplying a real source/region later is a seed-only change.

## Limitations

- Recurrence expansion, drag-to-reschedule, pinch zoom, and week-number displays are not implemented.
- Habit blocks are all-day by design; they do not occupy reminder-time slots.
- Month cells surface at most two chips plus a count; the full day content lives in the agenda below.
- Verification is compilation and debug APK assembly only (2026-09-08, BUILD SUCCESSFUL). No emulator, device, unit, end-to-end, or visual comparisons were run.
