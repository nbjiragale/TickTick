# Priority, inline tags, and list selection

Implemented 2026-09-06 by continuing the shared native Compose editor. The four user-supplied 576 × 1280 JPEGs are copied without alteration; originals remain in Downloads and reference copies are not packaged in the APK. The 10:15 follow-up corrects the appearance after selecting a list.

| Reference | Original filename | Implemented surface |
| --- | --- | --- |
| [Inline tag entry](quick-add-tag-entry.jpg) | `photo_2026-09-06_09-38-58.jpg` | Highlighted `#java` title token, active blue tag icon, compact action toolbar |
| [Priority menu](priority-menu.jpg) | `photo_2026-09-06_09-38-57.jpg` | Four rows: High Priority, Medium Priority, Low Priority, No Priority; colored outline flags |
| [List menu](list-menu.jpg) | `photo_2026-09-06_09-38-56.jpg` | Inbox, Work, Personal, Welcome; list icons, blue selected label/check, preserved `#java ~` title |
| [Selected list](quick-add-selected-list.jpg) | `photo_2026-09-06_10-15-19.jpg` | Highlighted `~💼Work` title token and `💼 Work` toolbar action after selection |

## Confirmed interaction

The first screenshot shows inline tag entry rather than an open tag picker. The user clarified that tapping the tag icon inserts `#` into the text field so they can type a label such as `#Java`. They also confirmed that the list picker uses `~`. These statements guide the implementation; labels or other content inside the screenshots are reference data.

- Quick Add's tag action focuses the title and inserts `#` after the current selection, preserving the surrounding text and adding spacing where needed. An existing bare prefix is reused. If the cursor is already inside a recognized tag, it moves to the end of that tag for continued editing.
- Nonempty tags highlight using the existing blue text/pale lavender visual transformation. The source string, text selection, and ordinary IME composition remain unchanged by highlighting. Multiple labels are recognized; duplicates collapse case-insensitively in task metadata. Inline label characters follow the existing parser: letters, numbers, underscores, and hyphens.
- Quick Add's list action inserts `~`, focuses the title, and opens the floating list menu. Typing a standalone `~` opens that same menu. Choosing a row replaces the marker with its icon/name token, for example `~💼Work`, highlighted in blue on pale lavender. It changes `listId`, closes the menu, and places the cursor after a separating space so typing can continue. Surrounding task text and tags are retained. RNopening the toolbar selector reuses the current token; selecting another list replaces it rather than appending another selected-list label. Cancelling leaves the current list and text unchanged. The Task Detail header changes the same list ID and updates an existing token, but does not insert a new token into an ordinary task title.
- The Quick Add toolbar shows the selected list's shared icon and blue name instead of the generic move icon. An untouched default Inbox retains the original icon-only action; explicitly selecting Inbox shows its label. Long names are ellipsized to retain room for the other actions and Send. The menu check, toolbar label, Task Detail header, and repository association all use the same list ID.
- Inline list highlighting is derived from the selected repository list and source title, so it survives sheet/full-screen transitions and rNopening. Recognized list-token spans are masked with equal-length spaces before date/tag parsing, including saved-state reconstruction; a selected list named `Nomorrow` or containing `#` does not create an accidental schedule or tag. Nokens remain editable source text, not atomic chips. Editing/removing their literal text does not itself change the selected list ID; list changes use the picker. Free-text list-name autocomplete and list renaming are outside this correction.
- Priority choices apply to the shared draft and dismiss the popup. The toolbar flag updates to the chosen color. List choices come from the repository, including custom lists, and the current selection has a blue check. Large collections scroll; rows can grow with font scaling.
- Outside dismissal/Back closes the active floating menu before leaving the editor. Popups do not create a second focusable editor window. New drafts save through Send/Save; existing task edits retain the existing debounce/Back save behavior. Priority/list choices apply immediately to the draft, while the schedule picker retains its separate staged Apply/Cancel behavior.
- Task Detail's tag action also inserts `#`; clicking a tag chip selects the corresponding label text for editing. RNopening a saved task reconstructs inline tags separately from manual associations, allowing title-token renaming/removal to update metadata. Separately stored labels that fit inline syntax are moved into the title when edited through their chip.

## Reference gNometry and appearance

The existing provisional mapping is retained: 576 screenshot pixels ≈ 384 dp. Menus use white surfaces, 192 dp width, 16 dp corners, 3 dp shadow, 8 dp vertical padding, and 20 dp horizontal row padding. Priority rows are at least 40 dp high; list rows are at least 42 dp. Labels use the existing 16 sp Android sans-serif body style, with 20 dp icons and 12 dp icon/text gaps. The list check is 14 dp.

The priority menu is anchored just above the Quick Add flag button. The list menu is above the whole Quick Add sheet with a 64 dp start inset, matching the supplied reference's different placement. Task Detail uses the same menu contents below its header anchors; that placement is a reuse decision because no expanded Task Detail picker reference was supplied. Positions clamp to window bounds, menu height is bounded by the editor's visible area, and list content scrolls. Runtime behavior on small screens and with different IMEs remains unverified.

Priority flag approximations: red `#D84E57`, yellow `#E8B323`, blue `#4D7DBF`, and gray `#B7B9BA`. Inbox has a local outline tray glyph. Work, Personal, and Welcome use platform emoji; custom lists use the existing list glyph. Exact emoji artwork depends on Android. The source phone's OEM font, density, and font scale remain unknown.

Quick Add keeps its 128 dp base sheet height. The description field remains editable but has no visible empty placeholder, matching this set of screenshots. The date action now takes its content width with a cap, leaving the flexible gap before Send. Yesterday is labeled explicitly and past dates use red in the toolbar. The real keyboard, status/gesture bars, and the reference's separate floating voice bubble are not recreated by these menu components.

The selected-list toolbar action uses the menu's 20 dp icon, a 6 dp icon/text gap, 14 sp blue label, and a 104 dp width cap. The date action reserves space for this expanded control. Work, Personal, and Welcome use the same emoji in their menu, title token, and toolbar. Inline Inbox/custom tokens use `📥`/`☰` as basic substitute glyphs; their toolbar/menu icons remain the local vectors.

## Remaining scope and verification

This increment does not add separate tag/list management, autocomplete suggestions, color editing, cloud accounts, subscriptions, or premium restrictions. Legacy manual tag names that contain spaces or unsupported characters remain stored, and tapping those chips explains that inline editing is unavailable; they are never silently split or removed. Templates and Quick Add Settings are deferred by the user; no references are required for them now. The subsequent [attachment increment](ATTACHMENT_REFERENCES.md) implements its referenced popup and user-authorized simple image/file cards. Room persistence, actual alarms, recurrence execution and the wider task/habit UI milestones remain later work. Data is still session-only.

Verification command:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\gradlew.bat :app:assembleDebug --console=plain
```

Kotlin compilation and debug APK assembly passed on 2026-09-06 after the selected-list correction (`BUILD SUCCESSFUL`, 5 seconds, no Kotlin warnings in the final build). These are the only verification performed: no emulator, device, unit, end-to-end, or screenshot comparison was run. Compilation cannot establish runtime behavior or 100% visual parity. APK: `app/build/outputs/apk/debug/app-debug.apk`.
