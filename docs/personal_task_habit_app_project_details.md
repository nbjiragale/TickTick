# Personal Task & Habit App - Project Details

## 1. Goal
Build a personal Android productivity app inspired by the TickTick experience, focused on the features actually used day to day.

The app should work fully offline, store data locally, provide reliable reminders, support recurring tasks and habits, and reproduce the captured UI and interaction patterns as closely as practical.

This is a personal-use project. No subscriptions, premium locks, cloud accounts, collaboration, or publishing-related features are required in V1.

## 2. Tech Stack
- Kotlin
- Jetpack Compose
- Room Database
- AlarmManager for exact reminders
- BroadcastReceiver for alarm events
- WorkManager only for maintenance/backup work
- Material 3 where useful
- Custom Compose components where needed to match the reference UI

Architecture:

```text
Compose UI
    ↓
ViewModel
    ↓
Domain / Use Cases
    ↓
Repository
    ↓
Room Database
```

## 3. V1 Scope
- Tasks
- Recurring tasks
- TickTick-style checklists
- Habits
- Reminder popup
- Snooze
- Real-time natural-language date/time parsing
- Tags
- Priorities
- Lists
- Attachments
- Today view
- Suggested Tasks
- Plan Your Day
- Local backup and restore
- Settings required for these features

## 4. Main Navigation
Bottom navigation:
1. Tasks
2. Calendar
3. Focus
4. Habits

For V1:
- Tasks: fully functional
- Habits: fully functional
- Calendar: minimal initially
- Focus: minimal initially

## 5. Navigation Drawer
Include:
- Profile area
- Search
- Notifications shortcut
- Settings shortcut
- Today
- Inbox
- Custom lists
- Task counts where applicable
- Add List
- Manage Lists

## 6. Home / Inbox
Main elements:
- Hamburger menu
- Current list title
- Three-dot menu
- Rounded white task container
- Checkbox or checklist icon
- Task title
- Due time/date on the right
- Reminder icon
- Floating blue add button
- Bottom navigation

## 7. Home Three-Dot Menu
Include:
- View
- Background
- Show Details
- Show Completed
- View Options
- Group & Sort
- Manage Section
- Select
- Share

## 8. Quick Add
The Quick Add experience is a major feature.

UI:
- Bottom sheet above keyboard
- Rounded top corners
- Existing task list remains visible behind it
- Title
- Description
- Quick-action toolbar
- Send/save button

Toolbar:
- Date/time
- Priority
- Tag
- List selector
- More
- Voice

More menu:
- Image
- Template
- Convert to Note
- Full-Screen
- Settings

## 9. Real-Time Natural Language Parsing
Parsing must happen while typing.

Example:

```text
Bring groceries tomorrow 10am
```

Expected:
- Task title remains unchanged
- `tomorrow` is recognized as the date
- `10am` is recognized as the time
- Recognized words are highlighted in blue
- A date/time chip appears immediately

Initial support:
- today
- tomorrow
- weekday names
- 10am / 10 am
- 9pm / 9 pm
- 9:30pm / 9:30 pm
- tomorrow 10am
- tomorrow at 10 am
- Monday at 9pm

Later in V1:
- every day
- every weekday
- every Monday
- every 2 days
- every week
- every month

Important interaction:
- Tapping the generated NLP date/time chip disables/removes the NLP-derived date/time
- It should not open the date picker
- Typed text stays untouched

## 10. Tags and List Parsing
Typing `#water` should recognize the tag live.

Typing `~` should open the list selector.

Example lists:
- Inbox
- Work
- Personal
- Welcome

## 11. Priority
Priority popup:
- High Priority
- Medium Priority
- Low Priority
- No Priority

Colors:
- High: red
- Medium: yellow/orange
- Low: blue
- None: gray

## 12. Task Detail
Quick Add Full-Screen and tapping an existing task should open the same reusable Task Detail screen.

Include:
- Back
- Current list name
- Priority
- More menu
- Date/time
- Reminder state
- Task title
- Description
- Checklist mode
- Tag
- Attachment
- Editing with keyboard

## 13. Checklist
Use TickTick-style checklist items only.

Do not build true nested subtasks in V1.

Each item:
- Checkbox
- Editable text
- Drag/reorder handle
- Divider

## 14. Attachments
Attachment menu:
- Take Photo
- Choose Photo
- Records
- File
- Scan Documents

Store file URI/path and metadata in Room. Do not store large binary files directly in Room.

Suggested metadata:
```text
id
taskId
type
displayName
uri
mimeType
createdAt
```

## 15. Date & Time
Date/time bottom sheet:
- Date tab
- Duration tab
- Calendar
- Time
- Reminder
- Repeat
- Clear

Duration:
- Start date
- End date
- Start time
- End time

No premium restriction.

## 16. Time Picker
Use a TickTick-like clock dialog:
- Hour
- Minute
- AM/PM
- Optional keyboard input
- Cancel
- OK

## 17. Reminder Configuration
Options:
- None
- On time
- 5 mins early
- 30 mins early
- 1 hour early
- 1 day early
- Custom

Custom:
- Days
- Hours
- Minutes

Prevent expired reminder values.

## 18. Constant Reminder
Available without premium restriction.

Possible behavior:
- Re-alert periodically until the task is completed, snoozed, or dismissed

## 19. Repeat / Recurrence
Default options:
- None
- Daily
- Weekly
- Monthly
- Yearly
- Every Weekday
- Custom

Custom Repeat:
- By Due Dates
- By Completion
- By Specific Dates

Frequency examples:
```text
Every 1 Day
Every 2 Weeks
Every 3 Months
```

Weekly recurrence should support weekday selection.

## 20. Reminder Popup
Highest-priority feature.

When a reminder fires, show a large rounded reminder card near the bottom of the screen.

Show:
- Due date/time
- List
- Task title
- Description
- Snooze
- Complete
- Close

It should work:
- When app is open
- When another app is open, where Android permits
- On lock screen, where Android permits

Use Android-supported APIs only.

## 21. Snooze
Options:
- 15 min
- 30 min
- 1 hour
- 3 hours
- Tomorrow
- Tomorrow Morning
- Next Hour
- Custom
- Change Date

Store snooze state in Room.

Task detail should display snooze state, for example:
```text
Sep 5, 9:48PM snooze until 10:18PM
```

## 22. Alarm Reliability
Must handle:
- Device reboot
- App process killed
- App closed
- Doze mode
- Battery optimization restrictions
- Exact alarm permission where applicable
- Timezone changes
- Device time changes

After reboot:
```text
BOOT_COMPLETED
    ↓
Read active reminders from Room
    ↓
Re-register AlarmManager alarms
```

## 23. Today
Show:
- Today's tasks
- Due times
- Checklist indicator
- Reminder indicator
- Floating add button

Top actions:
- Suggested Tasks
- Plan Your Day

## 24. Suggested Tasks
Show upcoming tasks that may be useful to move into Today.

Example:
```text
Upcoming

CALL yogya
Due tomorrow

Bring groceries tomorrow
Due tomorrow
```

Each task can have a `+` action.

## 25. Plan Your Day
Review tasks one at a time.

Actions:
- Done
- Today
- Later
- Won't Do
- Delete

Show progress:
```text
1/3
2/3
3/3
```

## 26. Lists
Add List:
- Name
- Color
- View Type
- Save
- Cancel

View types:
- List
- Kanban
- Timeline

List view is required in V1. Kanban and Timeline can come later if they slow down the core build.

## 27. Tags
Add Tag:
- Name
- Color
- Save
- Cancel

## 28. Habits
Habit main screen:
- Habit title
- Weekly date strip
- Selected date
- Habit list
- Empty state
- Add button
- Bottom navigation

## 29. New Habit
Basic:
- Name
- Icon
- Optional letter avatar
- Quote
- Quote refresh
- Next

Preset Gallery categories:
- Suggested
- Life
- Health
- Sports
- Mindset

Examples:
- Daily Check-in
- Drink water
- Eat breakfast
- Eat fruits
- Early to rise
- Early to bed
- Learn new words
- Read

Preset gallery is optional if it delays the core build. Custom habit creation is required.

## 30. Habit Frequency
Modes:

Daily:
- Select weekdays

Weekly:
```text
2 days per week
```

Interval:
```text
Every 2 days
```

## 31. Habit Goal
Goal:
- Achieve it all
- Reach a certain amount

Goal Days:
- Forever
- 7 days
- 21 days
- 30 days
- 100 days
- 365 days
- Custom 1-999 days

## 32. Habit Start Date
Calendar picker for habit start date.

## 33. Habit Sections
Default:
- Morning
- Afternoon
- Night
- Others

Manage Section:
- Add section
- Reorder sections

## 34. Habit Reminders
Support:
- Add reminder
- Multiple reminder times if needed
- Exact scheduling

## 35. Auto Pop-Up Habit Log
Habit configuration includes:
```text
Auto pop-up of habit log
```

When enabled, the app may automatically open the habit check-in UI at the configured reminder time.

## 36. Habit Completion
Full-screen habit check-in:
- Habit-specific background
- Illustration
- Habit title
- Quote
- Large completion control
- Back
- More menu

## 37. Habit Celebration
After completion:
- Celebration animation
- Confetti
- Achieved state
- Updated illustration
- Stats card

Stats:
- Total check-ins
- Best streak
- Current streak

## 38. Habit Streak Logic
Track:
```text
totalCheckIns
currentStreak
bestStreak
```

Must correctly support:
- Daily habits
- Selected weekdays
- Weekly targets
- Interval habits

## 39. Settings
Required:
- Tab Bar
- Appearance
- Date & Time
- Sounds & Notifications
- Widgets
- General
- Integrations & Import

Not required:
- Premium Account
- Subscription
- AI Features
- Recommend to Friends
- Follow Us
- Sign Out

## 40. Appearance
The supplied screenshots are the main visual reference.

Visual language:
- Very light gray/blue-gray page background
- White rounded cards
- Blue accent
- Minimal borders
- Soft shadows
- Large rounded bottom sheets
- Clean floating menus
- Roboto / Android system typography
- Generous spacing
- Consistent icon sizing

Refine spacing, radii, font sizes, and component heights against the screenshots.

## 41. UI Reference Rule
Do not redesign screens unnecessarily.

Use the supplied screenshots as the primary reference.

Only deviate when:
- Android platform rules require it
- A feature has no reference screenshot
- There is a real usability problem

Document assumptions where exact reference behavior is unavailable.

## 42. Room Database
Room is the source of truth.

Suggested entities:
```text
Task
ChecklistItem
TaskReminder
TaskRecurrence
TaskAttachment
TaskTagCrossRef
Tag
TaskList
Habit
HabitReminder
HabitCheckIn
HabitSection
AppSettings
```

Detailed schema can be designed separately.

## 43. Persistence
Room data survives:
- App close
- Force-stop
- Phone restart
- Phone reboot

Data may be lost if:
- User clears app storage
- User uninstalls the app
- Database is explicitly deleted

Therefore backup/restore is required.

## 44. Backup
Settings:
```text
Backup
    Export Backup
    Restore Backup
```

Backup should include:
- Tasks
- Checklist items
- Lists
- Tags
- Priorities
- Due dates
- Recurrence
- Reminders
- Snooze state
- Habits
- Habit reminders
- Habit history
- Settings

Recommended portable format:
```text
todo-backup-YYYY-MM-DD.json
```

Future richer backup:
```text
backup.zip
├── data.json
└── attachments/
```

## 45. No Cloud in V1
Do not require:
- Supabase
- Firebase
- Login
- Internet
- Account creation

Everything should work offline.

## 46. Permissions
Potential Android permissions:
- POST_NOTIFICATIONS
- SCHEDULE_EXACT_ALARM where applicable
- RECEIVE_BOOT_COMPLETED
- Camera
- Microphone
- Media/file access depending on Android version

Only request permissions when needed.

## 47. Suggested Package Structure
```text
com.niranjan.productivity
│
├── app
├── data
│   ├── database
│   ├── dao
│   ├── entity
│   ├── mapper
│   └── repository
├── domain
│   ├── model
│   ├── recurrence
│   ├── reminder
│   ├── nlp
│   └── usecase
├── feature
│   ├── home
│   ├── today
│   ├── quickadd
│   ├── taskdetail
│   ├── habits
│   ├── lists
│   ├── tags
│   └── settings
├── reminder
│   ├── AlarmScheduler
│   ├── AlarmReceiver
│   ├── ReminderActivity
│   ├── ReminderNotification
│   └── BootReceiver
├── backup
│   ├── BackupExporter
│   └── BackupImporter
└── ui
    ├── components
    ├── theme
    └── navigation
```

## 48. Development Order
```text
1. Project setup + theme
2. Room database
3. Lists + basic tasks
4. Inbox / Today UI
5. Quick Add
6. Live NLP parsing
7. Task detail
8. Date/time
9. Reminder scheduling
10. Reminder popup
11. Snooze
12. Recurring tasks
13. Checklist
14. Tags + priorities
15. Attachments
16. Reboot recovery
17. Backup / restore
18. Habits
19. Habit reminders
20. Habit streaks / celebration
21. Settings
22. Final UI matching / polish
```

Reminder reliability must be tested early, not left until the end.

## 49. V1 Completion Criteria
V1 is complete only when:
- Tasks persist after reboot
- Recurring tasks calculate correctly
- Exact reminders fire reliably
- Reminder popup works correctly
- Snooze reliably reschedules
- Snooze survives process death
- Alarms restore after reboot
- Live NLP works while typing
- Tags/list parsing works
- Checklists save and reorder correctly
- Attachments remain accessible
- Habits save correctly
- Habit reminders work
- Habit streaks calculate correctly
- Backup exports successfully
- Restore recreates data correctly
- Main UI closely matches supplied reference screenshots
- No subscription or internet connection is required

## 50. Out of Scope for V1
- User accounts
- Cloud sync
- Team collaboration
- Shared tasks
- Subscription system
- Premium paywalls
- AI assistant
- Complex analytics
- Social features
- iOS app
- Web app

## 51. Future iPhone Plan
Build Android cleanly now.

Later, an iOS version can reuse the product behavior and data concepts:
- Tasks
- Habits
- Recurrence
- NLP
- Local storage
- Backup

iOS reminder behavior must follow Apple's notification and lock-screen rules rather than forcing Android-style overlays.

## 52. Product Principle
The goal is not to rebuild every TickTick feature.

The goal is:

```text
A reliable personal task + habit app
with excellent reminders,
excellent snooze behavior,
fast task entry,
live NLP,
offline storage,
and the UI experience already preferred.
```

Reliability and daily usability are more important than feature count.
