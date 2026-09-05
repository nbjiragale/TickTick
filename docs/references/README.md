# First UI references

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

- Today shows `read java` at `9:00PM` and `ping Mahesh` at `10:00PM` in debug builds. Two upcoming Inbox tasks make the drawer's initial counts Today 2 / Inbox 4.
- Drawer opens from the hamburger or swipe, closes on selection/outside tap/Back, and filters the shared task records by Today or list.
- Task completion updates visible rows and drawer counts. Show Completed allows completed tasks to be restored.
- Simple create/edit dialogs, list creation, search, upcoming suggestions and a scheduled-task list provide working controls while fuller screenshots are pending. Times shown here are task due times; no Android reminders are scheduled.
- Four tab buttons are present. Calendar has a basic dated agenda, Focus has a local timer, Habits has only its empty shell. These do not yet claim reference fidelity or full feature completion.
- Basic view options/settings and list selection are wired; the full planned menu/settings scope remains future work.
- UI state is held in ViewModels/saveable state and an application-scoped in-memory repository. All actual task/list data resets when the app process restarts.

## Build and validation

The user requested compilation/build only, with no emulator or end-to-end testing. This restriction overrides the walkthrough steps in the broader UI plan for this increment. Build success cannot certify visual matching or runtime interactions.

Build command from the project root:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\gradlew.bat :app:assembleDebug --console=plain
```

The Gradle daemon uses the existing configured JDK 21 toolchain. Android Studio's local `jbr` installation is incomplete (`jvm.cfg` missing), so the available JDK 17 starts the wrapper instead.

The compile SDK was raised from 36.1 to the already installed 37.0 because existing Core 1.19.0 and Lifecycle Compose 2.11.0 require API 37. Minimum SDK remains 24 and target SDK remains 36. The Markdown brief was moved out of Android resources into `docs/` to fix the existing resource merger failure.

Output: `app/build/outputs/apk/debug/app-debug.apk`.

Next reference: Quick Add with the keyboard open, followed by Task Detail. The current simple dialogs should be replaced by those shared-editor presentations when references arrive.
