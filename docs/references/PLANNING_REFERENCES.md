# Plan Your Day reference — 2026-09-06

## Backend B5 update — 2026-09-25

The review date, candidate order, reviewed IDs and selected ID now persist in Room. Reopening on the same local day resumes the review; cold launch does not automatically open the planning dialog. New-day/review actions build fresh candidates, and resume refreshes the clock. Menus, confirmation prompts and animations remain transient. Planning Done, Won't Do, schedule changes and Delete commit the task and next review checkpoint together after earlier navigation writes drain. Recurring occurrence identities prevent an advanced occurrence from reappearing in the old review. Save failures retain the current card; checkpoint failures expose Retry through the existing error dialog. Task values remain live shared Room records. B3 recurrence and B2 reminders supersede the older deferred-backend statements below. Build/schema verification only; restart, failure, midnight and visual acceptance remain pending in the [backend plan](../BACKEND_IMPLEMENTATION_PLAN.md).

Source: `photo_2026-09-06_11-18-25.jpg`, copied unchanged to [plan-your-day.jpg](plan-your-day.jpg), 576 × 1280. The user confirms horizontal card sliding and defers Templates, Quick Add Settings and Suggested Tasks. Screenshot task names, dates and the `1/4` counter are reference data, not fixed app values.

## Implemented screen

Open from the Today lightbulb or Tasks overflow → Plan Your Day. A full-screen Compose dialog hides the shell, with X/Back exit, pale background, greeting/subtitle, large white rounded cards and five footer actions. HorizontalPager exposes the next card at the right edge. Swiping only browses; it does not apply an action.

Approximate geometry at 384dp width: 16dp heading inset, 20sp bold greeting, 14sp subtitle, 10dp card leading inset, 16dp card corners, 16sp date/list text, 22sp bold title, 14sp centered progress and a 68dp minimum footer with 22dp outline icons and 12sp labels. Cards fill available height; long titles/descriptions scroll inside them. System insets and dark system-bar icons on the light background are configured. Morning copy matches the reference; afternoon/evening copy is provisional.

Dates, overdue days, list names and the Today icon's day number derive from shared data and the injected clock. Existing debug seed records are preserved. List icons reuse local vectors.

## Actions and shared state

| Action | Current behavior |
| --- | --- |
| Done | Completes the displayed task and advances. |
| Today | Replaces the five footer actions with Morning 10 AM, Afternoon 1 PM, Evening 5 PM and Night 9 PM. Selecting one sets today's date and that time, then advances after a valid save. |
| Later | Replaces the footer with Tomorrow, 3 days later and Next week. Choices move the date by 1, 3 or 7 calendar days from today and preserve existing time, then advance after a valid save. The resolved date appears beneath each label. |
| Won't Do | Sets a distinct declined state and advances. Removes the task from active views/counts without marking it completed. |
| Delete | Confirms removal of the displayed task; Cancel preserves it. Source attachment files are not deleted. |

PlanYourDayViewModel observes the application-scoped TaskRepository; it does not maintain duplicate task data or depend on TaskEditorViewModel. PlanYourDayScreen reuses the design system; the corrected footer no longer opens SchedulePicker. At review start, the ViewModel snapshots active non-note IDs across all lists, ordered by due date/time with undated tasks last. Candidate order/membership stays stable while card values remain live. Completed, declined and externally deleted tasks disappear; new tasks enter on a new review.

The user supplied the Today/Later option names and times verbally because those premium screens cannot be captured. The original five icons disappear in place; text choices occupy the same footer. Opening a submenu makes no task change. The top-left arrow and system Back return to the main actions without saving. Swiping to another task cancels the submenu. Choice state belongs to the ViewModel and is bound to a task ID to prevent a stale choice being applied to a different card.

Both choice groups retain reminders and repeat configuration. Later shifts the duration's dates while preserving times and length. Today shifts a timed duration's start to the selected time and keeps its elapsed length; an all-day range becomes a timed interval of the same inclusive calendar-day count. Expired reminder validation keeps the options open and shows the existing error; it does not silently remove reminders or advance. Past Today slots remain available when the task has no conflicting reminder, consistent with existing scheduling rules. No alarms are delivered.

The counter is resolved count plus the current remaining-card position, over the initial total. Closing/reopening on the same day resumes the review. A new day or explicit Review again builds fresh candidates. ViewModel state survives ordinary configuration changes; process restart resets planning and the in-memory task repository. Empty and finished states provide Back to tasks and Review again. Repeated actions are guarded by task ID/eligibility; footer actions are disabled while the pager moves. Save errors retain the card.

Task.declined defaults to false. Task.isActive is shared by task/calendar filters, counts and planning. Completion/restoration clears declined; declining clears completed. The existing Show Completed option also exposes declined tasks, shown with an X and struck title. Tapping X restores them. Task Detail displays Won't Do with a restore control, and saved editor drafts retain the flag.

## Scope and limitations

- Only the supplied planning card is matched in code. Today/Later footer choices follow the user's verbal reference; their precise spacing and typography remain inferred. No additional premium screenshot is required for this behavior. Empty/finished states and afternoon/evening copy are provisional.
- Recurrence execution is deferred: Done/Won't Do affect the whole current task record and preserve its repeat configuration. They do not generate or skip an individual recurring occurrence yet.
- No Room, alarm delivery, cloud accounts, subscriptions or premium gating is added.
- Templates and Quick Add Settings entries are hidden; their earlier basic code remains dormant. Suggested Tasks has been removed from the current overlay/entry path.

The implementation uses the pager's settled page for action targeting; see [Android pager documentation](https://developer.android.com/develop/ui/compose/layouts/pager).

## Verification

Compilation and debug APK assembly after the Today/Later footer correction succeeded on 2026-09-06 (`BUILD SUCCESSFUL`, 7 seconds, no Kotlin warnings). Command: ` .\gradlew.bat :app:assembleDebug --console=plain` with `JAVA_HOME=C:\Program Files\Java\jdk-17`. Output: `E:\TickTick\app\build\outputs\apk\debug\app-debug.apk`.

Verification is restricted to compilation and debug assembly. No emulator, device, unit, end-to-end, runtime UI or visual comparison tests were run. Build success does not establish runtime correctness or visual parity.
