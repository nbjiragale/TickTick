# Task view, background, details and grouping — 2026-09-08

## Backend B5 update — 2026-09-25

Task destinations now persist List/Kanban, detailed rows, grouping, sorting/direction, background type/swatch/image URI and completed visibility in Room. Today, lists, tags and saved filters keep separate settings. The selected task destination/home section also persists. Preference changes publish after commit and failures use the existing error surface. Image selections retain the existing persisted URI grant and unavailable-image feedback. Calendar has its own completed visibility. Search, counts and groups continue to derive from the shared task repository; date/zone changes refresh on resume and at midnight without copied task records. B2 already persists the daily-alert pause; B4 persists habit settings. See the [backend plan](../BACKEND_IMPLEMENTATION_PLAN.md). Compilation/schema checks passed; restart, provider, UI and visual behavior remain unverified. This supersedes the historical session-only settings statements below.

## References and scope

The five original 576 × 1280 Telegram screenshots were copied unchanged from Downloads:

| Original | Reference | Surface |
| --- | --- | --- |
| photo_2026-09-08_18-31-05.jpg | [task-view-list.jpg](task-view-list.jpg) | View, List selected |
| photo_2026-09-08_18-31-06.jpg | [task-view-kanban.jpg](task-view-kanban.jpg) | View, Kanban selected |
| photo_2026-09-08_18-31-04.jpg | [task-background-gradient.jpg](task-background-gradient.jpg) | Gradient background and detailed rows |
| photo_2026-09-08_18-31-03.jpg | [task-group-sort.jpg](task-group-sort.jpg) | Group & Sort and compact rows |
| photo_2026-09-08_18-36-44.jpg | [task-overflow.jpg](task-overflow.jpg) | Task overflow |

The user states there is no task Manage Section and excludes View Options and Share. These entries are omitted. Habit section management and habit sharing retain their earlier scope. Select remains future work; no disconnected entry is exposed. Templates, Quick Add Settings and Suggested Tasks remain deferred.

The View references supersede the earlier list-only roadmap: both List and Kanban now work. The user's subsequent correction restores habits alongside tasks in the home List view only; Kanban remains task-only. Plan Your Day remains on the Today lightbulb; it is no longer an extra row in the reference menu.

## List habits and daily alert switch — follow-up

The latest user instruction supersedes the earlier task-only first-tab rule for List layout. A separate white Habit card follows the task groups in the home List view and can appear even when there are no tasks. Today and Overdue show today's scheduled habits (habits do not accumulate overdue records); Upcoming shows scheduled habit occurrences over the next seven days. Inbox/custom task lists retain their task-list filter because habits do not belong to task lists. The existing Habit Settings → Show in Today & Next 7 days preference controls this home card. Archived/unscheduled habits are excluded, check-in sorting is reused, and completed occurrences follow Show Completed. Tapping a row opens the existing check-in for its date; Back, archive and delete return to the originating task list. Kanban and Calendar are unchanged by this placement correction.

A 48 × 28 dp bell switch with a 52 × 48 dp touch area sits immediately left of Plan Your Day. Off uses a pale track and a white bell thumb on the left; On fills the track blue and slides the bell thumb right. Position, fill and icon tint animate for 220 ms using Compose's animation system, which respects the platform animation duration scale. Switch semantics announce its on/off state and today's date.

Daily alerts initially used an enabled-today date and returned to off on each new day. The September 11 pause-duration correction below supersedes that day-rollover behavior. This remains UI state only: no alarm registration/cancellation, notification permissions, popup suppression or debug-preview delivery logic is implemented. Durable settings and reliable system-wide day rollover belong to the later backend.

## Notification pause duration — 2026-09-11

Long-pressing the bell switch opens exactly two actions: Enable for today / Disable for today (according to current state) and Custom. A normal tap retains direct switching; a long press opens the menu without also toggling. The control retains its animated bell and switch accessibility state, with an accessible long-click action.

Custom opens a compact dialog asking how many days to disable notifications. A numeric input accepts whole days from 1 to 3650; invalid/blank input cannot Apply. The preview updates immediately while typing: "Notifications resume from [weekday, date, month, year]." Calendar days include today, so 1 day resumes tomorrow; on September 11, 3 days resumes September 14. Apply stores the pause; Cancel/Back make no change. Reopening shows the remaining pause days, and ordinary configuration recreation preserves entered text.

TasksViewModel now stores an exclusive local resume date in SavedStateHandle. Before that date the switch is off; on/after it, the switch is on. The midnight-aware clock flow updates the displayed state. Disable for today sets tomorrow as the resume date; Enable for today or tapping an off switch clears the current pause and resumes immediately. A manually chosen new pause replaces the previous one. The previous switch's current state is retained when initializing this preference: an enabled-today value stays enabled, otherwise the initial pause lasts through today. Automatic resumption replaces the earlier perpetual daily-off default. These are session UI preferences and have no effect on Android notifications/alarms/popups until backend integration.

## Behavior

- The rounded top-right menu offers View, Background, Show/Hide Details, Show/Hide Completed and Group & Sort. View expands into List/Kanban radio choices. Choosing a layout returns to the main menu rows. Expanded View has a Back handler; outside dismissal changes no preference.
- List uses continuous white grouped cards with headings/counts; None removes the headings. Kanban shows the same groups as horizontal columns with separate cards. Both reuse shared completion, task-editor and date-editor callbacks. Cross-column drag-and-drop is outside this increment.
- Show Details reveals the optional description, full date/time, reminder indicator and right-aligned list label. Hide Details returns to compact title/date rows. Checklist, attachment and tag editing remains in Task Detail; no extra textual metadata row is inserted into this reference layout. Long text is bounded; notes and priorities use local icons/colors.
- Group by: List, Date, Created Time, Tag, Priority, None. Date collects overdue tasks and separates other dates. Creation dates use the injected clock's time zone. Tasks with several tags appear under each tag, backed by the same record. Missing dates/tags/priorities have named fallback groups.
- Sort by: Date, Created Time, Modified Time, Title, Tag, Priority. Direction applies within groups; group order stays stable. Date/timestamp controls use Oldest/Newest First, text uses A–Z/Z–A, and priorities use High/Low First. Missing dates/tags and No Priority stay last.
- Background provides None, Color, Gradient and Image tabs. None restores the app default. Seven pastel swatches affect the Tasks shell, including header and bottom bar. Image opens Android's image document picker and requests persisted read access; the background uses a crop and light overlay for text legibility. Cancel keeps the previous background. Picker-access errors appear in the sheet; unreadable images fall back to the default with a recovery message. Remove image returns to None.
- Layout, details, grouping, sorting and background are saved per Today/Inbox/custom-list destination in SavedStateHandle. They survive tab switches and ordinary configuration recreation. Show Completed keeps its existing shared behavior, including Calendar. These are session preferences, not a durable settings store.
- The repository assigns creation/modification timestamps using the injected clock. Existing fixtures without metadata get stable timestamps around session initialization, in their existing order; these are not historical creation dates. Saves preserve creation time. Changed content, completion, decline and schedule updates update modification time; unchanged autosaves preserve it.
- Attachment thumbnail decoding was moved unchanged into the design system for reuse by image backgrounds: off-thread, EXIF-aware, bounded to an 800-pixel longest edge. No bitmap bytes enter saved state.

## Visual interpretation

Show Details correction (2026-09-08): reinspection of `task-background-gradient.jpg` identified incorrect vertical centering of the checkbox, a left-aligned list label in an oversized weighted area, undersized date text, excess date-line padding, and an unreferenced metadata row. Detailed rows now align the checkbox/note icon to the first title line. At default font scale, a single-line title plus date uses 66 dp total row height; a single description line adds 22 dp. Title/description/date share the same left edge with 2 dp line gaps. The date uses 14 sp text without the former 32 dp minimum/padding; the 12 sp list label sits at the trailing edge on the same baseline and is bounded for long names. Detailed rows use a 12 dp trailing inset and the reference-style yellow rounded note glyph. Compact row geometry retains its existing behavior. These values are derived from the reference and source review; rendered parity remains unverified.

The existing provisional 576 px ≈ 384 dp mapping is retained. The popup is capped at 244 dp with 22 dp corners. Group & Sort uses a pale sheet and three-column rounded choices in the supplied order. Background uses four segmented tabs and seven rounded swatches; it leaves the live preview undimmed.

None/Color/Image expanded content, multiple Kanban groups and metadata beyond the supplied examples use the existing design style. Original icon assets and exact font metrics are unavailable. Visual parity is not claimed.

## Verification

Notification pause and sidebar icon removal follow-up (2026-09-11): `:app:assembleDebug -Pkotlin.incremental=false -Pkotlin.compiler.execution.strategy=in-process --console=plain` passed (`BUILD SUCCESSFUL`, 2m 18s). Verification was limited to compilation and APK assembly; long-press behavior, keyboard interaction, automatic resumption and visual appearance remain unverified on a device.

List habits / daily alert switch follow-up: debug Kotlin compilation and APK assembly passed (`BUILD SUCCESSFUL`, 55s) using `:app:assembleDebug -Pkotlin.incremental=false -Pkotlin.compiler.execution.strategy=in-process --console=plain`. No Kotlin warnings were reported. The existing build-only constraint was retained: animation, check-in navigation, midnight reset and visual parity have not been verified on a device.

Show Details follow-up: `:app:assembleDebug --console=plain` passed (`BUILD SUCCESSFUL`, 1m 15s). The existing Kotlin daemon/cache error triggered Gradle's fallback compiler, which completed the build. This correction was checked against the supplied reference and source only; the APK has not been visually verified on a device.

Debug Kotlin compilation and APK assembly passed on 2026-09-08 (`BUILD SUCCESSFUL`, 1m 12s, no Kotlin warnings). Overlapping initial build attempts caused an incremental-cache conflict; the final single build used `:app:assembleDebug -Pkotlin.incremental=false -Pkotlin.compiler.execution.strategy=in-process --console=plain` with the existing JDK configuration. The flags apply only to that command; project Gradle settings were not changed.

Output: `E:/TickTick/app/build/outputs/apk/debug/app-debug.apk`. No emulator/device, unit, end-to-end, image-picker or screenshot tests were run under the existing build-only constraint. Runtime and visual acceptance remain pending.

## Durable alert switch — 2026-09-18

B2 moves the daily alert switch and exclusive local resume date into Room, importing prior SavedState values once when available. Fresh initialization preserves the previous default pause until tomorrow without resetting it at each launch. Pause changes await storage and the custom dialog stays open on failure. Alarm/notification reconciliation honors pause and resumes at the stored date. Other task presentation preferences remain session state. See [backend plan](../BACKEND_IMPLEMENTATION_PLAN.md); runtime midnight/permission behavior remains unverified.
