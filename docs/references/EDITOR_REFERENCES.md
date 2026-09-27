# Quick Add and Task Detail reference implementation

## Backend B5 update — 2026-09-25

Task drafts now use versioned Room payloads with owner/session/version and base revision, while retaining the existing editor UI. Text/selection, tags, checklist, attachment metadata, note/status, manual schedule, NLP suppression and the resolved prediction date/time survive recovery. Sheet/full-screen identity is retained; unapplied child-picker choices never replace the parent draft. Relative expressions keep their previously resolved dates until the recognized expression changes. Existing task edits still autosave; successful save atomically retires the acknowledged draft, preserving newer typing. An older draft cannot overwrite a newer task revision, restore a discarded version or recreate a deleted task. Unreadable draft payloads remain stored with explicit recovery/discard feedback.

Interrupted attachment imports restore a retry message with the earlier attachments intact; jobs and media bytes are not serialized. Application-owned draft writes continue after editor disposal, with failure/Retry feedback. Task-editor deletion retires its draft in the task transaction. Managed files/provider recovery remain B6. The root waits for data and draft storage before restoring editors. See the [backend plan](../BACKEND_IMPLEMENTATION_PLAN.md) for schema/build evidence. Runtime restart, failure and visual behavior remain unverified under the build-only restriction; older session-only persistence statements below are superseded.

Implemented 2026-09-06. The screenshots supplied on September 5 are preserved without alteration:

| Reference | File | Main implementation |
| --- | --- | --- |
| Inbox | [inbox.jpg](inbox.jpg) | Contiguous white task container, outer corners, inline due labels and reminder indicator |
| Empty Quick Add | [quick-add-empty.jpg](quick-add-empty.jpg) | Rounded sheet above the real IME; title, description and muted action toolbar |
| Quick Add with NLP | [quick-add-nlp.jpg](quick-add-nlp.jpg) | Highlighted source words, expanded date action, blue Send control |
| Quick Add More | [quick-add-more.jpg](quick-add-more.jpg) | Floating white menu above the toolbar; Image, Template, Convert to Note, Full-Screen, Settings |
| Task Detail | [task-detail.jpg](task-detail.jpg) | White page, list selector, flag/more, completion/date row, bold title, bottom tag/checklist/attachment actions |

## Visual implementation

The same provisional 1.5 screenshot-pixel/dp mapping is used as in the first increment. The basic Quick Add sheet is 128 dp tall: 16 dp top inset, title/description fields, spacing, and a 48 dp toolbar. It expands for multiline input or attachment/checklist summaries. Insets position the sheet above the device keyboard; SwiftKey itself and its toolbar are not app graphics.

Quick Add has 20 dp top corners, a 32% black window dim, 16 sp title input, 14 sp description/date labels, and a 40 × 30 dp blue Send pill. The floating More menu is 196 dp wide with 40 dp menu rows, 18 dp corners and a soft shadow. Task Detail uses a 22 sp bold title and the existing sans-serif theme. Exact OEM font identity and screenshot pixel parity remain unverified.

Inbox has a visually continuous card with 16 dp side insets and 16 dp outer corners. Rows remain lazy for long lists. The suggestion icon is only shown on Today. Debug Inbox now contains ping Mahesh, CALL yogya, and read java, with the reminder marker on ping Mahesh; Today sorts its two tasks by time.

## Shared draft and interactions

- A single `TaskEditorViewModel` owns title/description text and selection, list, date/time, priority, tags, checklist, attachment metadata and note state.
- Quick Add and full screen are presentations of the same editor session. Expanding does not save or duplicate a task. Back from a new full-screen task returns to Quick Add.
- A new task saves only through Send/Save. Dismissing a changed new draft asks whether to discard. Existing tasks save valid edits after a short debounce, and Back flushes pending changes before closing. Save failure preserves the draft.
- Saved-state reconstruction stores text, selection and small metadata, never attachment bytes. Actual tasks still use session-only repository storage; persistent drafts/database storage are later work.
- A local parser recognizes today, tomorrow, weekday names, AM/PM times and combinations with optional `at`. Source text and cursor offsets are preserved through a visual transformation.
- Tapping an NLP date action suppresses that recognized expression and keeps the title unchanged. Unrelated edits do not reapply it. Changing the expression enables recognition again. Manual date/time selection takes precedence.
- `#tags` are recognized without removing text. The tag button now inserts `#` into the title, and Quick Add's list button inserts `~` and opens the shared floating list menu. A typed standalone `~` opens the same menu. Selecting a list replaces the marker with its highlighted icon/name token, such as `~💼Work`, and displays the icon/name in the toolbar. Changing lists replaces that token while preserving surrounding typed text. See [organization notes](ORGANIZATION_REFERENCES.md) for the September 6 references and the user's interaction clarification.
- Attachment actions now use the reference popup, external camera, and system document pickers; image/file/audio cards appear inside the task and retain URI/type/size metadata. Files can be opened or deleted from the task; removing metadata does not delete the source file. Quick Add and Task Detail share the card renderer and import state. See [attachment notes](ATTACHMENT_REFERENCES.md) for the inferred card layout, scope of Records/Scan Documents, and storage limitations.
- Checklist items can be added, edited, completed, removed, and reordered with basic up/down controls. Templates append checklist items without overwriting a nonempty title. Note conversion preserves existing content.

## Deliberately basic until further references

Templates and Quick Add Settings are deferred by the user as of 2026-09-06. Their earlier basic dialogs remain in source but their menu entries are hidden. Priority and list selection use the reference floating menus; inline `#` entry replaces the comma-separated tag dialog. Date/time uses the shared calendar, duration, clock, reminder, and repeat screens described in [schedule reference notes](SCHEDULE_REFERENCES.md). The attachment popup and simple image/file cards are implemented. Actual Android alarm scheduling, recurrence execution/parsing, in-app audio recording, automatic document scanning, drag handles, and durable media ownership remain later work.

The empty toolbar's microphone action explains how to dictate using the keyboard. No microphone permission or background recording is started. Exact microphone integration needs a subsequent UI/platform pass.

One editor window hosts both sheet and full-screen presentations to preserve the editing session and IME ownership. The screen bodies are separate reusable composables; app-level Navigation 3 routing remains a subsequent integration step.

## Verification

Task Detail snooze follow-up, 2026-09-07: the user verbally approved showing "Snoozed until…" while retaining the original due time, with durations measured from the tap/confirmation time. The full-screen editor now reads the task's deadline from the shared snapshot and shows a blue Snooze icon and wrapping status line below the date row. It reuses the reminder formatter, remains outside draft serialization, and updates during an open session or on reopening. Existing snooze invalidation rules remove the status when appropriate. No further screenshot is needed for this state. Compilation/debug assembly passed in 1m 15s; runtime and visual behavior remain unverified.

The subsequent reminder-card increment adds protection for editor interruption. `TaskEditorViewModel` observes external task schedule/status changes and reconciles them again immediately before save. A reminder completion or date change cannot be overwritten by the editor's stale status/schedule; typed text, checklist/attachment edits and selection are preserved. An open date picker closes if its underlying schedule is replaced. `ReminderHost` presents the supplied card above the editor without beginning another editor session. Snooze deadlines remain in the repository outside serialized editor task fields. These are implementation behaviors, not runtime verification.

Only Kotlin compilation and `:app:assembleDebug` are used, as requested. No emulator, device walkthrough, screenshot comparison, unit test, or end-to-end test is run for this increment. This documents intended implemented behavior; a successful build is not runtime or pixel-parity evidence.

Date/time, priority, inline tag entry, list selection and attachment menu references have now been supplied and implemented. The user explicitly permits a simple attachment card layout because its reference is unavailable. Templates and Quick Add Settings do not need references now. Work has moved to [Plan Your Day](PLANNING_REFERENCES.md); Today/Later now follow the user's verbal description because premium screenshots are unavailable. Separate list/tag management and collapsed Task Detail remain later scope.
