# UX Flows and Initial Wireframes

## Navigation model

The Android application uses a single-activity Compose architecture. Top-level destinations are:

- **Home** — floor overview and fast access to rooms and devices
- **Activity** — device events and usage summaries
- **Alerts** — safety and operational notifications
- **Settings** — home configuration, floor management, and account actions

Device details and floor editing are contextual destinations reached from Home or Settings rather than
permanent navigation items.

The current authenticated dashboard introduces two task-focused tabs as an incremental navigation step:

- **Devices** — alerts, confirmed status, pending feedback, and direct power/channel controls.
- **Layout** — floor selection, grid editing, rooms, placement, and device creation.
- **Profile** — authenticated account identity and the deliberate sign-out action.

This keeps frequent monitoring actions visible without mixing them into the longer administration flow.
The Devices tab uses a high-contrast home header, compact Online/Active/Alerts summaries, profile icons,
and rounded state cards. User-facing labels resolve floor and room names; document IDs remain diagnostic
metadata and are not primary mobile copy.

Sign-out is located in Profile instead of the home header. This reduces the chance of an accidental
session-ending action during daily control and groups account actions with account context.

### Monitor a mock camera snapshot

```text
Devices → Cameras filter → Camera card → Loading → Snapshot / unavailable fallback
```

The camera card labels URI media as a mock snapshot, never as a live feed or recording. Its loading and
error text remains visible without relying on color, and the loaded image has an accessibility
description containing the device name.

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
Home → Select floor → Edit layout → Drag across cells → Name room → Apply
                                    Long-press cell → Choose device profile → Configure → Add
```

Direct manipulation is the primary path. Visible Add room and Add device actions open equivalent forms
for accessibility, precise correction, and gesture discoverability. Coordinates remain logical grid
values rather than pixels.

### Add a device

```text
Long-press empty cell → Select profile → Enter name/configuration → Review placement → Add
                    ↘ visible Add device button → select coordinate → same form ↗
```

Tapping a marker opens its summary. Edit and Move are explicit actions so a normal control tap cannot
accidentally change placement.

### Respond to a safety cutoff

```text
Backend turns device off → Alert appears → User opens alert → Device/event details
```

The alert explains what happened, when it occurred, which configured limit was exceeded, and the final
device state.

### Configure a light schedule

```text
Devices → Light card → Edit schedule → Enable → Enter ON/OFF time and timezone → Save
```

Times use strict 24-hour `HH:mm` values. A start later than the end represents an overnight window, such
as `18:00–06:00`. Manual switching remains available; the next evaluator run reconciles the light with
its configured schedule.

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
│ Drag empty cells to add a room    │
│ Hold a cell to add a device       │
│ [+ Room]  [+ Device]  [Edit]      │
└──────────────────────────────────┘
```

### Add or edit room fallback

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

## Direct-manipulation behavior

- A short tap selects a room/device or clears the current selection.
- A drag starting on an empty cell previews a room rectangle; release opens naming/confirmation.
- A long press on an empty cell opens device creation at that coordinate.
- Drag and long-press thresholds use platform gesture detection rather than custom timing constants.
- Preview, selected, invalid, and committed states have distinct outline patterns and text descriptions.
- Scrolling outside the editor must not accidentally create a room; the editor claims input only after
  the drag gesture is recognized.
- Haptic feedback may confirm long-press recognition but is never the only feedback.
- Profile creation uses progressive disclosure: common name/placement fields remain visible, while only
  the selected profile's channels, safety duration, or mock media fields are shown.

## Accessibility baseline

- Do not communicate device status by color alone.
- Provide semantic labels for device icons and switches.
- Keep touch targets at least 48 dp.
- Support font scaling without truncating safety-critical information.
- Announce command result and important alert changes to accessibility services.
- Require confirmation for destructive floor removal and configuration changes with safety impact.
