# Task selection — 2026-09-11

## References and scope

The user supplied five screenshots and requested long-press task selection with five bottom actions:

- [Date shortcuts](task-selection-date.jpg): `photo_2026-09-11_20-09-24.jpg`.
- [Move to Another Section](task-selection-section.jpg): `photo_2026-09-11_20-09-23 (2).jpg`.
- [Move to list](task-selection-move.jpg): `photo_2026-09-11_20-09-23.jpg`.
- [Yellow Undo button](task-selection-undo.jpg): `photo_2026-09-11_20-09-22.jpg`.
- [More actions](task-selection-more.jpg): `photo_2026-09-11_20-09-25.jpg`.

The earlier Share exclusion remains in force. No task/list management screen is reinstated. Skip the Recurrence remains disabled because occurrence execution is deferred. The screenshot's long-press shortcut-customization hint is omitted: no shortcut editor behavior or references were requested. New priority/tag picker layouts use the existing design language.

## Implemented behavior

- Long-press a task in either List or Kanban to select it. Further row/checkbox taps select or deselect without completing or opening it. The header shows the count, Back exits, and an accessible Select All button selects the visible tasks. The task overflow also has Select. Tag-group duplicates share one selected ID and count once.
- Selection replaces normal navigation with Date, Move to Another Section, Move to, Delete, and More. The toolbar slides up and fades in over 260 ms. Add task is hidden while selecting; habits are outside task selection. Changing the home date tab exits selection.
- Date opens the centered rounded six-choice card. Today, Tomorrow and the next strictly future Monday preserve each task's time and duration length. Pick Date opens the shared Date/Duration picker with a sliding entrance and applies the chosen schedule to all selected tasks. Clear removes date, time, duration, reminder and repeat configuration. Dismissing the picker returns to shortcuts without applying.
- Move to Another Section uses the active grouping. Date groups offer Overdue and Today plus displayed future date sections. Overdue assigns yesterday; Today assigns today. List/priority/tag groupings offer their respective destinations; choosing a tag replaces the task's group tags and reconciles removed inline tags. Created Time explains that groups follow the creation timestamp. These additional grouping behaviors are local interpretations beyond the supplied date-group screenshot.
- Move to opens a rounded bottom sheet with Search, the shared list catalog, a check when all selected tasks belong to a list, and Add List. Add List uses the existing editor and returns to the picker without navigating away from selection. Choosing a list updates all selected tasks. Existing inline list tokens agree with the destination.
- Delete removes the selected batch immediately, exits selection and shows the yellow circular Undo button above bottom-left navigation. Undo restores the last deleted batch once, including original IDs, contents, checklist/attachment references, timestamps, pin state, positions and session snooze deadlines. It remains until used or replaced by a subsequent deletion during this session; there is no invented timeout. No attachment files are deleted.
- More slides up above the right edge of the toolbar without a dim overlay. Done, Pin/Unpin, Set Priority, Tags, Duplicate and Convert to Note/Task update shared data. Pin sorts first within each group and has a small row indicator; it survives editor saves. Mixed tags use an indeterminate checkbox; untouched tags are preserved and explicit removals reconcile inline tag tokens. Duplicates have new task/checklist/attachment IDs, reset completion/checklist progress and no copied snooze deadline; attachment URIs remain shared.
- Actions read current repository records and exit selection after applying. Batch saves/deletion/restoration emit one shared snapshot. Draft controls apply on confirmation; Back, outside dismissal and Cancel preserve tasks.

## Recurrence backend update — 2026-09-21

B3 enables Skip the Recurrence when every selected task is active and repeating. Skip records history, advances one occurrence, resets checklist completion and clears snooze atomically. Bulk Done uses the same transition; stale revisions reject the whole batch. Delete/Undo retains recurrence metadata/history. This supersedes the earlier disabled/deferred recurrence status. See the [backend plan](../BACKEND_IMPLEMENTATION_PLAN.md) for build evidence and pending runtime acceptance.

## Verification and limitations

Only Kotlin compilation and debug APK assembly are permitted under the user's existing constraint. No unit, device, emulator, runtime or screenshot tests were run. Visual parity, touch/keyboard behavior and animation smoothness remain unverified on a device. Notification delivery, recurrence execution and durable persistence remain future backend work. Selection IDs survive saved-state recreation; deletion Undo stays in the retained view model for this session.

Final assembly after all refinements passed (`BUILD SUCCESSFUL`, 2m 14s) using JDK 17 and `:app:assembleDebug -Pkotlin.incremental=false -Pkotlin.compiler.execution.strategy=in-process --console=plain`. Source whitespace checks also passed. Earlier intermediate builds passed; no runtime checks were performed.

APK: `E:/TickTick/app/build/outputs/apk/debug/app-debug.apk`.
