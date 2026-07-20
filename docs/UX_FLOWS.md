# UX Flows and Initial Wireframes

## Navigation model

The Android application uses a single-activity Compose architecture. Top-level destinations are:

- **Home** — floor overview and fast access to rooms and devices
- **Activity** — device events and usage summaries
- **Alerts** — safety and operational notifications
- **Settings** — home configuration, floor management, and account actions

Device details and floor editing are contextual destinations reached from Home or Settings rather than
permanent navigation items.

## Primary user flows

### Monitor and control a device

```text
Sign in → Home → Select floor → Select device → Review status → Request state change
                                                       ↓
                                           Pending → Confirmed / Failed
```

The interface distinguishes a requested command from a confirmed physical state. Controls become
temporarily non-ambiguous while a command is pending and show a recoverable error when confirmation
fails.

### Create a floor layout

```text
Settings → Floors → Add floor → Set name and grid size → Add rectangular rooms
                                                     → Save → Place devices
```

Room creation begins with explicit numeric grid coordinates and dimensions. Dragging and resizing can
be added later without changing the underlying room model.

### Respond to a safety cutoff

```text
Backend turns device off → Alert appears → User opens alert → Device/event details
```

The alert explains what happened, when it occurred, which configured limit was exceeded, and the final
device state.

## Initial mobile wireframes

### Sign in

```text
┌──────────────────────────────────┐
│            Smart Home            │
│                                  │
│  Email                           │
│  ┌────────────────────────────┐  │
│  │                            │  │
│  └────────────────────────────┘  │
│  Password                        │
│  ┌────────────────────────────┐  │
│  │                            │  │
│  └────────────────────────────┘  │
│                                  │
│  ┌────────────────────────────┐  │
│  │          Sign in           │  │
│  └────────────────────────────┘  │
│           Error area             │
└──────────────────────────────────┘
```

### Home and floor dashboard

```text
┌──────────────────────────────────┐
│ Smart Home              profile  │
│ [Ground] [First]        [+ floor]│
├──────────────────────────────────┤
│ Kitchen          │ Living room   │
│                  │               │
│  ◉ Main light    │  ◉ Camera     │
│  ◉ Outlet        │               │
├──────────────────┼───────────────┤
│ Utility          │ Bedroom       │
│  ⚠ Iron          │  ◉ Switch x3  │
│                  │               │
├──────────────────────────────────┤
│ Home     Activity  Alerts  Settings│
└──────────────────────────────────┘
```

Device markers use profile-specific icons and a separate status treatment. Color is supplementary;
status remains readable through icon shape, label, and accessibility description.

### Device details

```text
┌──────────────────────────────────┐
│ ← Utility iron                   │
│                                  │
│ Status                 ON        │
│ Connected              Yes       │
│ Last confirmed         10:42     │
│ Active for             08:14     │
│ Safety limit           15 min    │
│                                  │
│  ┌────────────────────────────┐  │
│  │          Turn off          │  │
│  └────────────────────────────┘  │
│                                  │
│ Recent activity                  │
│ • Turned on by user       10:34  │
│ • Reconnected             09:58  │
└──────────────────────────────────┘
```

### Floor editor

```text
┌──────────────────────────────────┐
│ ← Edit floor              Save   │
│ Name       [Ground floor      ]  │
│ Grid       [12] × [16]           │
├──────────────────────────────────┤
│ ┌──────────────┬───────────────┐ │
│ │ Kitchen      │ Living room   │ │
│ │              │               │ │
│ ├──────────────┼───────────────┤ │
│ │ Utility      │ Bedroom       │ │
│ └──────────────┴───────────────┘ │
│                                  │
│ [+ Add room]  [Place device]     │
└──────────────────────────────────┘
```

### Add or edit room

```text
┌──────────────────────────────────┐
│ Room                             │
│ Name       [Kitchen           ]  │
│ Column     [0]   Row       [0]   │
│ Width      [5]   Height    [6]   │
│                                  │
│ Layout preview / validation      │
│ "Room does not overlap"         │
│                                  │
│ [Cancel]              [Apply]    │
└──────────────────────────────────┘
```

### Alerts

```text
┌──────────────────────────────────┐
│ Alerts                    [Read]  │
├──────────────────────────────────┤
│ CRITICAL  Utility iron           │
│ Switched off after 15 minutes    │
│ Today, 10:49                     │
├──────────────────────────────────┤
│ WARNING   Garage camera          │
│ Device disconnected              │
│ Today, 09:15                     │
├──────────────────────────────────┤
│ Home     Activity  Alerts  Settings│
└──────────────────────────────────┘
```

### Activity and usage

```text
┌──────────────────────────────────┐
│ Activity       [7 days ▾]        │
│ Device         [All ▾]           │
├──────────────────────────────────┤
│ Safety devices                   │
│ Iron       4 activations  42 min │
│ Heater     2 activations  55 min │
├──────────────────────────────────┤
│ Recent events                    │
│ Iron automatically off    10:49  │
│ Main light turned on      10:30  │
├──────────────────────────────────┤
│ Home     Activity  Alerts  Settings│
└──────────────────────────────────┘
```

## Simulator layout

The browser simulator favors diagnostic clarity over matching the mobile UI:

```text
┌──────────────────────────────────────────────────────────────┐
│ Smart Home Hardware Simulator      Cloud: Connected          │
├──────────────┬───────────────────────────────────────────────┤
│ Floors       │ Ground floor                                  │
│ • Ground     │ Device        Profile       State    Actions  │
│ • First      │ Main outlet   Outlet        OFF      [ON]     │
│              │ Switch gang   3-channel     ONLINE   [1][2][3]│
│ Filters      │ Utility iron  Safety        ON       [OFF]    │
│ Status       │ Camera        Camera        ONLINE   [ERROR]  │
│ Profile      │                                               │
│              │ [Disconnect] [Reconnect] [Report error]       │
└──────────────┴───────────────────────────────────────────────┘
```

## Required state variants

Each data-backed screen must define:

- Initial loading
- Loaded with content
- Loaded but empty
- Recoverable listener/network error
- Offline with cached data
- Permission denied or session expired

Device controls additionally define idle, command pending, command confirmed, command rejected, error,
and disconnected states.

## Accessibility baseline

- Do not communicate device status by color alone.
- Provide semantic labels for device icons and switches.
- Keep touch targets at least 48 dp.
- Support font scaling without truncating safety-critical information.
- Announce command result and important alert changes to accessibility services.
- Require confirmation for destructive floor removal and configuration changes with safety impact.
