# TickTick — offline Android task and habit app

A Kotlin/Jetpack Compose app for tasks, recurring schedules, habits, calendar planning and Focus sessions. Data stays in a local Room database; no server or account is required.

## Build

Use JDK 21, Android SDK platform `android-37.0`, Build Tools `36.0.0`, and the checked-in Gradle 9.4.1 wrapper. Set `ANDROID_HOME` or put `sdk.dir=/absolute/path/to/android-sdk` in an untracked `local.properties`.

```sh
sdkmanager 'platforms;android-37.0' 'build-tools;36.0.0'
./gradlew :app:assembleDebug :app:assembleRelease --console=plain
```

The app supports Android 7.0/API 24 and later, targeting API 36. Installable development APK: `app/build/outputs/apk/debug/app-debug.apk`. Release output: `app/build/outputs/apk/release/app-release-unsigned.apk`; configure your own release signing before distribution. GitHub Actions builds and uploads both variants. The current repository verification policy is build-only: CI does not run tests or devices.

## Local workflows

- Tasks, recurrence history, habit quantities, organization, editor drafts and settings persist across restarts. Task and habit edits use revision checks to prevent stale saves from overwriting newer work.
- Open a custom list, tag or saved filter, then use the task menu to edit or delete it. Deleting a list moves tasks to Inbox. Deleting a tag removes its inline tokens and assignments; saved tag conditions remain until edited. Inbox is protected.
- Settings includes sounds, reminder access, navigation visibility/order, a light-theme accent, week start, 12/24-hour display, default task list and new-task date-only reminder time. Existing reminders retain their saved schedule.
- Focus stores the active timer and session history. Same-boot process recovery uses monotonic elapsed time; reboot recovery pauses at the last checkpoint, which can omit up to roughly 30 seconds. Completed Pomo sessions and stopped Stopwatch sessions contribute to totals; interrupted Pomo records remain visible. Background completion uses Android alarms with an inexact fallback and recovery work. Sound and keep-screen-on are active only while the Focus screen is open.
- Settings → Backup & restore exports JSON containing saved records and attachment metadata. Media bytes and provider access grants are excluded. Import validates a bounded file in a staging database before replacing saved data. A pre-restore recovery copy is retained and can be exported in Settings, then imported through the normal restore flow. Restored running timers are paused. Missing media remains identifiable through its saved metadata.

Android notification permission, channel settings, exact-alarm access, popup access, DND and volume determine actual reminder delivery. Review the app's reminder settings on the device. Fresh release installs start with Inbox and no sample tasks or holidays.

See [implementation audit and device acceptance checklist](docs/IMPLEMENTATION_AUDIT.md), [backend delivery tracker](docs/BACKEND_IMPLEMENTATION_PLAN.md), and [reference notes](docs/references/README.md) for current scope and deliberate deferrals. Build success is not a claim of device or visual acceptance.
