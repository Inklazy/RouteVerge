# RouteVerge Design System

version | 1.0
name | RouteVerge Calm Navigation
platform | Android · Jetpack Compose · Material 3
description | A quiet, map-first control surface for route simulation and fixed-point simulation. Content and map context lead; controls remain clear, stable, and easy to stop.

## Overview

RouteVerge uses a bright, low-noise interface inspired by Apple's restrained hierarchy: generous whitespace, near-black typography, one confident action blue, soft neutral surfaces, and minimal decoration. The map is the primary object. Controls should explain the current state without competing with it.

## Design principles

- Map first: route and point previews occupy the main visual area.
- Stable controls: switching modes or starting simulation never relocates the primary action.
- One clear action: blue is reserved for the current primary action and selected interactive controls.
- Calm hierarchy: status, map, configuration, saved records, and tools appear in that order.
- Whole-row interaction: saved records are selected by tapping the complete row; avoid ambiguous affordances.
- Safe stopping: every running mode exposes an obvious stop action while preserving the stable two-button route control row.

## Colors

Token | Value | Use
--- | --- | ---
`primary` | `#0A6ED1` | Primary buttons, selected links, map-selection action
`primary-pressed` | `#0759AA` | Pressed state
`primary-container` | `#E5F1FF` | Selected speed option and subtle selection surfaces
`on-primary` | `#FFFFFF` | Text and icons on primary controls
`canvas` | `#FFFFFF` | Main page background
`surface` | `#FFFFFF` | Cards and map container
`surface-soft` | `#F7F8FA` | Secondary grouping and inactive surfaces
`surface-selected` | `#E2EAF5` | Selected segment background
`ink` | `#1D1D1F` | Primary text
`ink-muted` | `#6F7378` | Secondary text, labels, metadata
`ink-subtle` | `#979BA1` | Disabled and tertiary text
`hairline` | `#E1E5EA` | Card and control borders
`status-ready` | `#35C875` | Ready/verified status indicators only
`priority-low` | `#B8BDC5` | Low-priority switches
`priority-medium` | `#737A84` | Medium-priority switches
`priority-high` | `#3D444D` | High-priority switches
`danger` | `#D64545` | Destructive or stopped-error state

Do not use gradients, decorative shadows, or additional accent colors. The green status dot for Alipay NFC remains semantic and is not part of the switch-priority palette.

## Typography

Use the Android system sans-serif family. Prefer `FontWeight.Normal` and `FontWeight.SemiBold`; avoid heavy bold text.

Token | Size | Weight | Line height | Use
--- | --- | --- | --- | ---
`screen-title` | 32sp | 400 | 1.15 | RouteVerge title
`section-title` | 22sp | 400 | 1.25 | Saved routes/points, Tools
`body` | 17sp | 400 | 1.45 | Main labels and actions
`body-strong` | 17sp | 600 | 1.3 | Selected item/name
`caption` | 14sp | 400 | 1.4 | Metadata and WGS-84 labels
`status` | 18sp | 400 | 1.3 | Current status text

Use tabular numerals for coordinates, distances, speeds, and counts.

## Layout

- Base spacing unit: 8dp.
- Screen horizontal padding: 36dp on the reference phone; use responsive 16–24dp on narrower devices.
- Vertical rhythm: 8, 16, 24, 32, 48dp.
- Current-status card: large rounded container, 24dp internal padding, clear status badge on the trailing side.
- Mode selector: full-width pill, 56dp minimum height, selected half filled with `surface-selected`.
- Map preview: full available width, approximately 280–360dp height depending on device, 32dp corner radius, 1dp `hairline` border, no heavy shadow.
- Primary buttons: 56dp minimum height, pill radius, centered icon and label. Route actions use a stable two-column row: secondary new/pause/continue on the left and primary start/stop on the right.
- Two-column controls such as coordinates and point actions use equal flexible columns with 16dp spacing.
- Respect system bars and bottom gesture insets; content must remain scrollable.

## Components

### Current status card

Shows `当前状态`, a concise state description, and a trailing status badge. The badge is informational; it must not shift the card's text when its value changes.

### Mode selector

The `定点 / 路线` selector stays directly below the status card on both pages. The selected segment uses `surface-selected` and a check icon. Switching mode preserves the selector's position.

### Map preview

Reuse the existing real map component. Route mode displays the selected saved route. Point mode displays the selected map coordinate or saved point. Empty states should explain the next action without fake map data.

### Primary action

Blue, pill-shaped. `开始路线` starts route simulation. `开始定点` starts point simulation. While point simulation is running, the same location displays `停止模拟`. Route simulation uses `暂停模拟`/`继续模拟` beside `停止模拟`; the two buttons keep the same position and width in every route state.

### Saved records

Saved route and point rows are full-width tappable surfaces. The entire row selects the record; do not render a right-side expand/select triangle. Keep independent play and overflow actions where they exist, with adequate touch targets.

Point names are optional. If empty, generate `点位1`, `点位2`, etc.; otherwise preserve the entered name. Store latitude and longitude with the point.

### Tools

Tools remain after saved records. Alipay NFC jump is always visible at the highest layout layer and must not be covered by scroll content. Its green dot communicates verification/enabled state; the trailing chevron communicates navigation.

## Interaction states

State | Route action | Point action
--- | --- | ---
Ready | `开始路线` | `开始定点`
Running | `暂停模拟` available; `停止模拟` ends run | `停止模拟`
Paused | `继续模拟` and `停止模拟` | Not applicable
Stopped | Return to ready state | Return to ready state

Starting or stopping must not move the primary button. Pause applies only to route simulation and means remaining at the current route point until resumed.

## Accessibility and motion

- Minimum touch target: 48dp.
- Maintain readable contrast for all text and controls.
- Provide content descriptions for settings, map selection, play, stop, pause, and overflow icons.
- Use short 150–250ms state transitions. Do not animate layout reordering.
- Announce simulation state changes for accessibility services.

## Implementation guidance

Keep shared components and tokens in the existing theme/design-system layer. Prefer Material 3 components with explicit colors, shapes, and typography. Keep business state separate from presentation state so route pause state cannot leak into point simulation. Validate on small and large portrait screens, with long saved-record lists and with the NFC tool enabled.
