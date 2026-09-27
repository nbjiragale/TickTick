# Date, duration, reminder, and repeat references

## Backend update — 2026-09-21

B3 connects repeat controls to durable advancement/history. Due-date rules retain their anchor and advance one slot per Complete/Skip; completion-based rules use the local completion date (Skip uses the due date); specific dates are finite. Month/year clamping and reminder-only edits retain the original date anchor. Unchanged overdue schedules can be confirmed; new/changed expired reminders are rejected. See the [backend plan](../BACKEND_IMPLEMENTATION_PLAN.md) for exact policies and evidence. This supersedes older configuration-only/deferred-backend statements below. Runtime and visual acceptance remain pending.

Implemented 2026-09-06 from the seven supplied 576 × 1280 JPEGs. Originals remain unchanged in Downloads; these reference copies are outside Android resources and are not packaged in the APK.

| Reference | Original filename | Implemented surface |
| --- | --- | --- |
| [Date sheet](date-sheet.jpg) | `photo_2026-09-06_09-03-21.jpg` | Date/Duration tabs, Sunday-first calendar, month navigation, selected/today circles, three option rows, Clear |
| [Time picker](time-picker.jpg) | `photo_2026-09-06_09-03-22.jpg` | Hour/minute dial, AM/PM, keyboard entry, Cancel/OK |
| [Reminder picker](reminder-picker.jpg) | `photo_2026-09-06_09-03-23.jpg` | Multiple offsets, date-only 09:00 labels, Custom, Constant Reminder, Cancel/OK |
| [Repeat picker](repeat-picker.jpg) | `photo_2026-09-06_09-03-24.jpg` | None, daily, weekly, monthly, yearly, weekdays, Custom |
| [Custom repeat](custom-repeat.jpg) | `photo_2026-09-06_09-03-25.jpg` | Repeat basis, snapping frequency wheels, weekday pills |
| [Repeat basis menu](repeat-type-menu.jpg) | `photo_2026-09-06_09-03-27.jpg` | By Due Dates, By Completion, By Specific Dates |
| [Duration sheet](duration-sheet.jpg) | `photo_2026-09-06_09-03-28.jpg` | Start/end date and time, duration summary, all-day switch, reminder/repeat configuration |

## Visual implementation

The same provisional scale as earlier references is used: 576 screenshot pixels correspond to approximately 384 dp. Sheet background is sampled `#F7F7F7`, with white cards/dialogs, blue selections, muted gray secondary text, and red Clear. The sheet fills the bottom 74% of the window, uses 20 dp top corners, and accounts for the actual system navigation inset. Native Android system bars remain device-rendered.

Calendar cells use 43 dp week rows and 38 dp selection circles. Option rows are at least 49 dp tall. Dialogs use 326 dp content width, 20 dp corners, and 24 dp horizontal text insets. The clock face is 250 dp with 40 sp digits above it. Custom frequency uses a 230 dp card and scrollable, snapping wheels. Typography uses the app's Android sans-serif family without extra letter spacing; the source phone's exact OEM font and font scale are still unknown. Icons use the existing local vector set plus clock/repeat/keyboard/chevron glyphs.

The brief explicitly excludes premium restrictions. Consequently Duration and Constant Reminder are enabled, and the reference's Upgrade Now button and premium crown are omitted. Custom reminder fields, duration range selectors, and the specific-date calendar have functional layouts inferred from the brief because their expanded screenshots have not yet been supplied.

## State and behavior

- Quick Add, Task Detail, and task-row due labels open the same schedule picker. Tapping the task title still opens Task Detail. NLP chip suppression retains its existing separate behavior.
- The picker stages a copy of the task schedule. Check applies the result; X, Back, and outside dismissal discard uncommitted changes. Clear removes date, time, duration, reminders, and repeat together. Child dialog Cancel leaves the parent selection unchanged; preset repeat choices apply to the staged schedule.
- Date and Duration tabs share the start date/time while retaining the temporary range. Duration supports different start/end days, validates ordering, and retains time values while All day is enabled. Moving a duration task to Today shifts its entire date range.
- Clock input supports hour and minute selection by tap/drag, AM/PM, and a keyboard alternative. Start/end dialogs have distinct input state. Keyboard values are restricted to hours 1–12 and minutes 00–59.
- Date-only tasks offer on-the-day and day/week offsets using 09:00 by default. Timed tasks offer on time, 5/30 minutes, 1 hour, and 1 day early. Custom offsets accept days/hours/minutes; date-only custom reminders can change their base time. Multiple offsets are supported and duplicate offsets collapse. Expired reminders cannot be applied. Constant Reminder is saved as a preference only.
- Repeat presets derive weekday/day/month labels from the selected date. Custom rules retain basis, interval (1–999), unit, weekdays, or an explicit set of dates. Switching rule types removes incompatible values on Apply. Specific dates are bounded to 366 and reminders to 32 for reconstruction-state size.
- Domain configuration is plain Kotlin. Editor reconstruction uses JSON, while local picker selections use saveable state. Tasks, including schedules, still live in the shared in-memory repository and reset on process restart. This is not durable storage.

Actual Android alarm delivery, repeated alerts, recurrence advancement after completion, snooze, and Room persistence are later milestones. This increment implements configuration UI and session data; it does not claim those runtime services exist.

## Verification

The seventh increment adds a centered presentation of this same `SchedulePicker` for [Snooze → Change Date](REMINDER_REFERENCES.md). It follows `snooze-change-date.jpg`: Date/Duration header tabs, the shared calendar/options, Time/Reminder clear controls and bottom Clear/Cancel/OK. Clear is staged in this variant, so Cancel preserves the task and its snooze; OK applies the valid schedule and clears obsolete snooze state. The existing editor's sheet presentation and immediate Clear action retain their previous behavior. The centered variant reuses all existing duration/time/reminder/repeat controls and introduces no new schedule model.

Run only `:app:assembleDebug` with the existing JDK setup described in [reference notes](README.md). Kotlin compilation and debug APK assembly passed. No emulator, device run, automated UI test, unit test, or screenshot comparison was performed, per the user's instruction. Build success does not establish runtime correctness or 100% visual parity.

The subsequent increment replaces the basic priority and list dialogs and confirms inline `#` tag entry; see [organization reference notes](ORGANIZATION_REFERENCES.md). Expanded template, attachment, settings and list/tag management surfaces remain pending. Schedule behavior is unchanged by that increment.
