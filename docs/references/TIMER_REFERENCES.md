# Timer references — 2026-09-12

## Source and approved scope

- [Idle Pomo](timer-idle.jpg): `photo_2026-09-12_12-09-21.jpg`.
- [Running focus](timer-running.jpg): `photo_2026-09-12_12-09-25.jpg`.
- The user identifies the running screen's sun icon as Screen Always On, enabled/disabled.
- The user explicitly approved simple working dialogs for controls without supplied opened-screen references: statistics, +, music, Focus and More.

## UI and interactions

The old centered timer placeholder is replaced by `feature/focus/FocusScreen.kt` and `FocusViewModel.kt`. The idle layout has Pomo/Stopwatch tabs, statistics, duration (+), More, a task/Focus selector, thin pale circular ring, 25:00 by default, and a blue Start pill. The Timer navigation glyph is now the concentric-circle symbol. The existing app's four destinations/order are preserved; the source app's extra bottom destination is outside this increment.

Start expands the timer and hides normal bottom navigation. The top-left downward chevron minimizes without stopping. The running header contains the sun and More; below the ring are bordered music/stop controls and a blue pause button. The ring uses elapsed progress and a blue endpoint. Pause changes to Resume, freezes elapsed time and shows Paused. Stop confirms before resetting; Cancel preserves the session. At Pomo completion the ring fills, Session complete appears and Start Again begins a fresh session. The focus-note action sits at the bottom of the expanded screen. Mode/duration changes are unavailable during a running or paused session.

The provisional screenshot scale remains 576 px ≈ 384 dp. Background is #F3F3F9, action blue #3975FF and ring #E7E8ED. Ring diameter is 74% of screen width (bounded for larger/shorter windows), with 5 dp stroke and 44 sp digits. Main vertical positions are shared between idle and expanded layouts by reserving the usual navigation-bar space. Short screens can scroll. These values are source-derived, not verified rendered parity.

The approved provisional dialogs provide:

- Duration: 1–180 whole minutes, validated on Save; Cancel leaves the duration unchanged.
- Focus: choose an active task or plain Focus. Selecting a task labels the session and does not complete or edit the task.
- Notes: editable session text with Save/Cancel.
- Statistics: completed session count and total completed focus minutes in this app session. Stopped stopwatches count; interrupted Pomo sessions do not count as completed.
- Music: Silent or a user-selected audio file. The document picker grants read access; MediaPlayer loops it only while running and the Timer screen is resumed. Pause, leaving Timer, completion and backgrounding release playback. Unavailable/unplayable files show a recovery message. No bundled sound-library UI is inferred from the screenshot.
- More: screen-always-on toggle, duration, sound and minimize/expand when applicable.

## State and device behavior

Timer timing uses elapsed realtime with a ViewModel retained across tab switches. Session configuration, elapsed baseline, mode, expanded state, selected task, note and totals use SavedStateHandle. A background service, alarm, completion notification, durable session history and recovery across device reboot are not implemented. Returning while the retained session is active reflects elapsed time; this is not a promise of background alarm delivery.

Screen Always On is a real foreground display setting via the current view's keepScreenOn property. The sun has switch semantics and blue enabled state. The setting is applied while Timer is visible/resumed (including paused/idle if the preference remains enabled), released on leaving/backgrounding and reapplied on returning. Audio is optional and never starts unless the user has selected a file and starts/resumes focus. Neither screen-on nor audio was exercised on a device under the build-only constraint.

## Verification

Compilation/debug APK assembly is the only authorized verification. An initial Kotlin build found nested Compose receiver references; explicit captured width fixed them. Final `:app:assembleDebug -Pkotlin.incremental=false -Pkotlin.compiler.execution.strategy=in-process --console=plain` passed with JDK 17 (`BUILD SUCCESSFUL`, 1m 3s); source whitespace checks passed. No unit, emulator, device, runtime or screenshot tests were run. Visual parity, animation smoothness, screen-on behavior and audio playback remain unverified on a device.

APK: `E:/TickTick/app/build/outputs/apk/debug/app-debug.apk`.
