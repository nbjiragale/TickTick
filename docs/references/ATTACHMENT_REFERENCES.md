# Attachment menu and task cards

Implemented 2026-09-06 from [attachment-menu.jpg](attachment-menu.jpg), copied without alteration from `photo_2026-09-06_10-39-34.jpg` (576 × 1280). The user confirmed that added images/files should appear inside the task and that delete is sufficient for attachment actions. The reference app's premium restriction prevented an added-attachment screenshot; it is not a requirement for this app. No premium checks, quotas, subscriptions, or accounts were added.

## Reference menu

The paperclip in Task Detail now opens a floating white menu directly above its toolbar anchor. Rows appear in the supplied order: Take Photo, Choose Photo, Records, File, Scan Documents. The menu reuses the editor's popup positioning and row components, uses 196 dp width, 16 dp corners, 3 dp shadow, 8 dp vertical padding, 40 dp rows, 20 dp horizontal padding, 20 dp outline icons, 12 dp icon/text gaps, and 16 sp labels. The provisional 1.5 screenshot-pixel/dp mapping remains in use. It scrolls if the available height is constrained and dismisses through outside tap or Back while retaining the draft.

The screenshot includes a collapsed Task Detail sheet behind the menu. This increment updates the menu in the existing full-screen Task Detail presentation; it does not implement that additional collapsed/expandable detail presentation. Exact device typography, small-screen behavior, and pixel matching remain unverified.

## Implemented actions

| Action | Current behavior | Limit |
| --- | --- | --- |
| Take Photo | Launches the device camera with an app-owned JPEG destination, then attaches the returned image | Requires an available camera app; no in-app camera UI |
| Choose Photo | Opens the system document picker filtered to images | Provider permission/access can change later |
| Records | Opens the picker filtered to existing audio recordings | Does not record audio or request microphone permission |
| File | Opens the document picker for any file type | Does not copy all imported bytes into managed storage yet |
| Scan Documents | Offers Photograph document or Choose existing scan (PDF/image), then uses the corresponding camera/picker action | No automatic document cropping, perspective correction, OCR, or multipage PDF generation |

These actions are invoked only when the user selects them in the installed app. No camera, picker, emulator, or device was launched during implementation. The chosen media/file remains attached to the same editor state used by Quick Add and Task Detail. Quick Add More → Image shares the image picker and card renderer.

## Attachment presentation and state

- Shared `AttachmentCards` displays image previews and compact file/audio cards in both editor presentations. This is a deliberately simple inferred layout, authorized by the user's unavailable reference: white cards, 12 dp corners, a faint border, filename, type, optional file size, and a trash button. Images fit within a 156 dp preview area without intentional cropping; filenames may wrap to two lines.
- Tapping a preview or filename opens the file using the existing system-viewer action. Unsupported/missing files report an error; an unavailable image thumbnail retains its file card and delete action. The trash button removes only the task association. It does not delete the user's original file or revoke permissions shared with other task attachments.
- Metadata is loaded on an IO dispatcher through an import job owned by `TaskEditorViewModel`. A loading label is shown and save waits for the import to finish. Imports survive ordinary configuration changes with the ViewModel; closing/replacing a draft cancels its job, and stale results for other task IDs are ignored. Source name, URI, MIME type and optional size enter the task/saved-state metadata; file bytes and thumbnail bitmaps do not.
- Thumbnails decode off the UI thread with an 800 px longest-edge target. API 28+ uses ImageDecoder; older supported APIs use sampled BitmapFactory decoding and EXIF orientation handling. Loading and unavailable-preview states are included.
- Photo capture uses `AttachmentFileProvider` with a non-exported manifest entry and a path limited to the app's private `files/attachments/` directory. A failed/cancelled capture removes only its temporary destination after checking the parent directory. Completed captures remain files even if their task association is later removed; orphan-file cleanup belongs to the durable attachment milestone.
- Task records still live in the application-scoped in-memory repository and reset on process restart. Provider URI grants and app-owned photo files do not constitute durable task persistence. Process death during an import, a managed-media lifecycle, reliable recovery, and backups remain later work.

## Platform references

The camera action uses AndroidX's contract for writing a captured image into a supplied content URI. [TakePicture API](https://developer.android.com/reference/kotlin/androidx/activity/result/contract/ActivityResultContracts.TakePicture).

The dedicated provider subclass and narrowly configured files path follow Android's documented file-sharing setup. [FileProvider API](https://developer.android.com/reference/androidx/core/content/FileProvider).

Bounded modern image decoding uses a target size set in the header callback. [ImageDecoder API](https://developer.android.com/reference/android/graphics/ImageDecoder).

## Verification and remaining references

Only Kotlin compilation and debug APK assembly are permitted. No unit, device, emulator, end-to-end, or screenshot-comparison checks are performed. A successful build does not establish runtime correctness or 100% visual parity. Command: `:app:assembleDebug --console=plain` with the existing JDK setup; output: `app/build/outputs/apk/debug/app-debug.apk`.

Final Kotlin compilation and debug assembly passed on 2026-09-06 (`BUILD SUCCESSFUL`, 10 seconds). The final build reported no Kotlin warnings. Camera/picker behavior and rendered appearance have not been verified on a device.

Templates and Quick Add Settings are now deferred by the user; work has moved to [Plan Your Day](PLANNING_REFERENCES.md). Additional recording/scanner references are needed when implementing those full capture interfaces. The added-attachment card screenshot is explicitly unavailable and is not blocking this increment.
