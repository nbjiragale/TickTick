# First UI references

September 12 Timer: [idle and running references](TIMER_REFERENCES.md) now drive the Pomo/Stopwatch layouts, ring, start/pause/resume/stop, minimize and screen-always-on toggle. The user approved simple functional dialogs for the remaining visible controls. Device/visual acceptance remains pending.

September 11 task selection: [five supplied references](TASK_SELECTION_REFERENCES.md) now cover long-press/overflow selection, the animated five-action toolbar, date shortcuts, section/list moves, yellow deletion Undo and More actions. Share remains excluded; recurrence execution remains deferred. Compilation/assembly verification is recorded in the selection notes.

September 11 corrections: the sidebar's bottom-right management icon and its route are removed at the user's request. The home bell toggle now supports long-press → Enable/Disable for today or Custom, with a day-count dialog and live resume-date preview. See [task-view notes](TASK_VIEW_REFERENCES.md) and [drawer scope](DRAWER_ADD_REFERENCES.md). Delivery backend integration remains pending.

Drawer Add increment (2026-09-09): [List, Filter and Tag screens](DRAWER_ADD_REFERENCES.md) follow the four supplied screenshots. The previous Add List dialog is replaced with full-screen creation; saved tags and filters are connected to live task views. Timeline and undefined More remain later work.

Snooze feedback follow-up (2026-09-08): the user requests a small two-second confirmation card after snoozing. Preset/custom success now shows the selected duration or resolved day/time; errors/cancellation do not show a success message. See [reminder notes](REMINDER_REFERENCES.md).

Latest follow-up (2026-09-08): home List view again includes habits alongside tasks per the user's revised placement instruction. Kanban is unchanged. A daily notification-bell switch is positioned left of Plan Your Day; it animates between off/left and filled/on/right and applies to today's UI preference only. Backend integration remains later work. See [task-view follow-up](TASK_VIEW_REFERENCES.md).

Task view increment (2026-09-08): [View, Background, Show Details and Group & Sort](TASK_VIEW_REFERENCES.md) follow five supplied screenshots. List and Kanban are functional; appearance and grouping/sorting are stored per task destination. The user excludes task Manage Section, View Options and Share. Select remains pending. Compilation/debug APK assembly passed; runtime and visual checks remain unverified.

Calendar views increment (2026-09-08): no calendar screenshots were supplied; the four-view Calendar tab (List/Day/Week/Month with soft category blocks, legend and sample-labeled holidays) follows the documented design decisions in [calendar notes](CALENDAR_REFERENCES.md).

Content placement correction (2026-09-08): the first Tasks tab now contains only tasks and Add. Calendar, the second tab, shows tasks and scheduled habits for the selected date. This replaces the earlier habit injection into Tasks → Today/Upcoming. See [updated habit notes](HABIT_REFERENCES.md).

Habit check-in increment (2026-09-08): tapping a habit now opens the [blue check-in screen and four-action overflow menu](HABIT_REFERENCES.md). Edit is behind the menu. Check-in/quantity progress and Undo update shared totals; Share opens Android's chooser, Archive preserves history, and Delete confirms removal. The original check-in and menu screenshots are saved in the reference folder.

Habit toolbar correction (2026-09-08): the second icon now opens the full-screen Active/Archived list; the third opens Habit Settings. [New reference notes](HABIT_REFERENCES.md) cover archive/restore, the ringtone picker, sorting, Today/next-seven-day visibility and full-screen section management. App-badge delivery remains pending in the notification layer.

Bottom navigation correction (2026-09-08): Tasks → Calendar → Habits → Timer. The third, pin-shaped icon opens Habits. Timer now uses a separate round clock icon in the fourth position.

Habit reminder correction (2026-09-08): the [multiple-reminder reference](HABIT_REFERENCES.md) adds wrapping 24-hour time chips, inline Add, and the missing Constant Reminder switch/crown row. The setting saves with the habit; repeated alarm delivery remains deferred with the reminder engine.

Latest Habits increment (2026-09-07): [Habits home and custom creation](HABIT_REFERENCES.md) uses the corrected eleven September 7 screenshots. It replaces the placeholder with date/section navigation and a two-step custom editor, including frequency, goals, start date and reminder preferences. Data remains session-only; check-in/celebration and actual reminder delivery are later work.

Latest reference increments (updated 2026-09-07): [the reminder card, Snooze, Custom Snooze and Change Date](REMINDER_REFERENCES.md) are implemented from the 18:21 and 18:40 screenshots. Task Detail now shows "Snoozed until…" below the original due time from the user's verbal approval; no additional screenshot is needed for this state. Debug entry: drawer → bell → Scheduled tasks → Preview Reminder; delayed and queued previews are also available there. Today Night uses 9 PM and disables at/after that time; Tomorrow preserves the task's original time. The card's Focus action is omitted at the user's request. `reminder-picker.jpg` documents reminder configuration, and the resent planning image remains a Plan Your Day reference.

User supplied 2026-09-05:

- [Today](today.jpg): `photo_2026-09-05_23-10-08.jpg`, 576 × 1280 pixels.
- [Navigation drawer](drawer.jpg): `photo_2026-09-05_23-10-07.jpg`, 576 × 1280 pixels.
- `photo_2026-09-05_23-10-02.jpg` shows the same Today layout and was not copied as a second baseline.

The original files remain unchanged in Downloads. These references belong to this project and are not bundled into the APK.

## Visual specification

The reference is treated as approximately a 384 dp wide phone at a 1.5 screenshot-pixel/dp ratio. Actual device density, font scaling, and OEM font have not been supplied. The layout uses native dp/sp and real system bars, so pixel parity requires a later comparison on the target device.

| Element | Implementation value | Evidence / assumption |
| --- | --- | --- |
| Page background | `#F3F4F9` | Sampled from multiple flat screenshot areas |
| Task card | `#FFFFFF` | Sampled |
| Today pill | `#E0E7F9` | Dominant sampled fill; JPEG compression produces nearby values |
| Primary blue | `#4778FF` | Approximation of the compressed blue control/icon palette |
| Due time text | `#557EB9` | Approximation; text edges are compressed and antialiased |
| Drawer gradient | `#EAEDFC` → pale blue → white | Sampled top/background |
| Typography | Android sans serif, no additional letter spacing or font padding | Platform Roboto where available; OEM font identity is unconfirmed |
| Screen heading | 20 sp, bold | Estimated from reference |
| Task title | 16 sp, normal | Estimated |
| Due labels and drawer rows | 14 sp | Estimated |
| Profile name | 16 sp, bold | Estimated |
| Task row | 44 dp minimum height, 12 dp corners | Reference geometry; grows with larger text |
| Task side margins | 22 dp | Approximately 33 reference pixels |
| Row gap | 8 dp | Estimated |
| FAB | 56 dp circle; blue gradient and soft shadow | Estimated; no default Material container |
| Bottom tabs | Icons only, no labels or active pill | Reference |
| Drawer | 494/576 of viewport width, square outer edge | Measured screenshot boundary |

Dynamic colors and automatic dark theme are disabled for this light reference. The visible OS clock, battery, connectivity symbols and gesture handle are system-rendered, not static app graphics.

Icons are a small local Canvas vector set. Work/Home use basic symbols; Welcome uses the platform waving-hand emoji. The gray avatar crown is decorative and has no account/subscription behavior. Exact icon artwork can replace each symbol without changing the layout.

## Implemented in this increment

- Today shows `read java` at `9:00PM` and `ping Mahesh` at `10:00PM` in debug builds. The second reference increment uses the three Inbox tasks shown in the newer screenshot, so current drawer counts are Today 2 / Inbox 3.
- Drawer opens from the hamburger or swipe, closes on selection/outside tap/Back, and filters the shared task records by Today or list.
- Task completion updates visible rows and drawer counts. Show Completed allows completed tasks to be restored.
- Shared Quick Add/Task Detail, list creation, search and a scheduled-task list provide working controls. Suggested Tasks is now deferred; the Today lightbulb opens Plan Your Day. Times shown here are task due times; no Android reminders are scheduled.
- Four tab buttons are present. Calendar has a basic dated agenda, Focus has a local timer, Habits has only its empty shell. These do not yet claim reference fidelity or full feature completion.
- Basic view options/settings and list selection are wired; the full planned menu/settings scope remains future work.
- UI state is held in ViewModels/saveable state and an application-scoped in-memory repository. All actual task/list data resets when the app process restarts.

## Build and validation

### Home date-section follow-up — 2026-09-06

The user requested Overdue and Upcoming in addition to Today. Home now exposes three selectable tabs in that order, using the existing rounded pale-blue selected pill, blue selected text, muted inactive labels and shared task cards. The supplied earlier screenshots establish the Overdue/Today pill style; Upcoming follows the user's text request. Precise parity for the expanded row has not been visually verified.

Overdue includes dated tasks before today; Today includes today's tasks; Upcoming includes all dates after today. Sections span all lists and sort by due date/time, with active records shown by default. Undated tasks remain in Inbox/their lists, and Show Completed also reveals completed/declined records in the selected date section. Each section has its own empty message. Date editing and planning choices move records between sections through the same repository. Selection is stored in SavedStateHandle; selecting Today in the drawer explicitly returns to the Today section. The drawer Today count remains today's active-task count.

This correction passed compilation and debug APK assembly (`BUILD SUCCESSFUL`, 6 seconds, no Kotlin warnings); no emulator, device, unit, end-to-end or visual comparison tests were run.

The user requested compilation/build only, with no emulator or end-to-end testing. This restriction overrides the walkthrough steps in the broader UI plan for this increment. Build success cannot certify visual matching or runtime interactions.

Build command from the project root:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\gradlew.bat :app:assembleDebug --console=plain
```

The Gradle daemon uses the existing configured JDK 21 toolchain. Android Studio's local `jbr` installation is incomplete (`jvm.cfg` missing), so the available JDK 17 starts the wrapper instead.

The compile SDK was raised from 36.1 to the already installed 37.0 because existing Core 1.19.0 and Lifecycle Compose 2.11.0 require API 37. Minimum SDK remains 24 and target SDK remains 36. The Markdown brief was moved out of Android resources into `docs/` to fix the existing resource merger failure.

Output: `app/build/outputs/apk/debug/app-debug.apk`.

Quick Add and Task Detail references were supplied and implemented in the next increment. See [editor reference notes](EDITOR_REFERENCES.md) for the new layouts, behaviors, and remaining scope.

The third increment adds [date/duration, time, reminder, and repeat screens](SCHEDULE_REFERENCES.md), including direct schedule editing from a task-row due label.

The fourth increment adds [priority/list menus and inline tag entry](ORGANIZATION_REFERENCES.md) from the three September 6, 09:38 screenshots. The user's clarification establishes that toolbar buttons insert `#` and `~` into the title; no separate tag-picker screenshot is missing for that entry interaction. These references use the existing shared editor and session repository. Compilation/debug APK assembly are the only permitted verification; no emulator, device, unit, or end-to-end tests are run.

The subsequent [selected-list reference](quick-add-selected-list.jpg), `photo_2026-09-06_10-15-19.jpg`, corrects the post-selection appearance: highlighted `~💼Work` in the title and `💼 Work` in the toolbar. The same organization notes track this correction, token replacement behavior, and remaining limitations.

The fifth increment adds the [attachment menu and task cards](ATTACHMENT_REFERENCES.md) from `photo_2026-09-06_10-39-34.jpg`. The user permits a simple image/file card with delete because the added-attachment reference is unavailable behind the source app's premium restriction. No premium restrictions are implemented. Camera/document selection is wired; Records imports existing audio and Scan Documents offers a document photo or saved PDF/image. In-app recording, automatic scanning and durable attachment storage remain later work.

The sixth increment adds [Plan Your Day](PLANNING_REFERENCES.md) from [the swipeable card reference](plan-your-day.jpg), `photo_2026-09-06_11-18-25.jpg`. Open it with the Today lightbulb or Tasks overflow. Done, Today, Later, Won't Do and confirmed Delete update the shared repository. The user's verbal follow-up defines replacement footer choices: Today offers 10 AM / 1 PM / 5 PM / 9 PM; Later offers Tomorrow / 3 days later / Next week. No premium screenshot is required for those options. Declined tasks can be restored through Show Completed or Task Detail. Templates, Quick Add Settings and Suggested Tasks are deferred by the user. Recurrence execution and runtime/visual verification remain deferred.
