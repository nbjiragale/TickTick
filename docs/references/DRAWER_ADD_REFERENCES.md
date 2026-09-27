# Drawer Add menu, List, Tag and Filter — 2026-09-09

## Supplied references

| Original screenshot | Local copy | Surface |
| --- | --- | --- |
| photo_2026-09-09_19-13-47.jpg | [drawer-add-menu.jpg](drawer-add-menu.jpg) | Bottom-left Add opens List, Filter, Tag |
| photo_2026-09-09_19-13-46.jpg | [add-list.jpg](add-list.jpg) | Add List, name/color/view choices |
| photo_2026-09-09_19-13-44.jpg | [add-tag.jpg](add-tag.jpg) | Add Tag, name/color, keyboard focus |
| photo_2026-09-09_19-13-45.jpg | [filter-reference.jpg](filter-reference.jpg) | Premium information with an Advanced filter example |

The source screenshots were copied unchanged from Downloads. The app's existing no-paywall scope applies: no subscription page or upgrade action is implemented. The Filter example guides a functional local editor; its expanded controls are an interpretation, not a claim of matching a supplied editor screenshot. System keyboard/status bars and the keyboard's floating voice bubble are rendered by Android, not duplicated inside the app.

## Implemented

- Drawer bottom-left Add opens a rounded white menu with List, Filter and Tag, in that order. Outside/Back dismiss the menu; selecting an item closes the drawer and opens its full-screen editor.
- Add List and Add Tag use the reference pale background, X/title/check header, gray rounded Name field, white color card, default/no-color swatch, six preset colors and a rainbow custom-color action. The custom dialog accepts a six-digit hex value and shows a preview before applying; Cancel keeps the old color.
- Add List includes stylized miniature List/Kanban/Timeline previews. List and Kanban select the actual initial layout of the saved list. Timeline is visibly unavailable with a Later label, consistent with the existing scope. No crown or premium restriction is added. The screenshot's collapsed More section is not implemented because neither its contents nor behavior are supplied or specified in the product brief.
- Add Tag initially focuses Name so the user's keyboard opens. Names may optionally begin with #; saved names use letters/numbers/underscores/hyphens so they work with the existing inline task-tag parser. List/tag/filter duplicates are checked without case sensitivity. Invalid input/save errors preserve the draft. X/Back ask before discarding entered changes; child dialog cancellation preserves the parent form. Name, color, selected view and filter-rule drafts survive ordinary configuration recreation.
- Saved lists appear immediately in the drawer and editor list selector. Their selected colors tint their icons. Saved tags appear in a new Tags section along with tags already present on tasks; tag selection opens a live filtered task view with the usual task actions. A new unused tag is retained in the catalog even when its result list is empty.
- Saved filters appear in a Filters drawer section. Both tag and filter counts derive from active shared tasks. Saving any item navigates to its task view, including when the drawer was opened from another tab.
- Normal filters combine at most one Date, Priority, List and Tag condition using AND; Any means that field is unrestricted. Each condition can be cleared. Advanced supports multiple editable/removable conditions, + Add, and a shared AND/OR operator. Switching back to Normal requires compatible rules, preserving unsupported advanced conditions with an explanatory error instead of dropping them.
- Date choices are Today, Tomorrow, This week, Next 7 days, Overdue and No date. This week is Sunday–Saturday, consistent with the current calendar; Next 7 days includes today plus six days. Date conditions are evaluated against the injected clock, not frozen when saved. Priority/list/tag choices use the current records. At least one condition and a name are required to save a filter. The Advanced example is This week AND High Priority, with an additional Today OR High Priority example.

## Data and scope

TaskList now holds optional ARGB color and initial List/Kanban view. TaskSnapshot contains explicit tag and saved-filter catalogs; implicit tags remain available through a merged, case-insensitive knownTags view. Pure FilterRule predicates power results and counts. Tag and saved-filter navigation use distinct TaskFilter variants. Existing Today, habit placement, task editor, reminders and per-view settings remain shared.

This increment covers creation and navigation. On 2026-09-11 the user explicitly skipped the bottom-right sidebar management screen: its icon, callback and obsolete manageLists dialog are removed. Rename/delete/reorder functionality through that screen is excluded from upcoming work. Expanded More, Timeline, advanced nested expression groups, durable storage, and visual acceptance remain later work. No real task tags are rewritten during tag creation. Selecting a tag filter does not automatically tag an unrelated task; use the existing # entry while editing a task.

## Verification

Final debug Kotlin compilation and APK assembly passed on 2026-09-09 (`BUILD SUCCESSFUL`, 1m 17s; no Kotlin warnings). Command: `:app:assembleDebug -Pkotlin.incremental=false -Pkotlin.compiler.execution.strategy=in-process --console=plain` with the existing JDK configuration. Output: `E:/TickTick/app/build/outputs/apk/debug/app-debug.apk`.

Verification remains compilation/debug APK assembly only. No emulator/device, keyboard, interaction, unit or screenshot comparison tests are run under the existing constraint. Geometry uses the existing provisional 576 px ≈ 384 dp mapping; visual parity remains unverified.
