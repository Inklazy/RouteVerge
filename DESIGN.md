# RouteVerge Design System

version | 2.0
name | RouteVerge Warm Navigation
platform | Android · Jetpack Compose · Material 3
description | A map-first control surface with Claude-inspired warm editorial surfaces, restrained motion, and stable simulation controls.

## Direction

RouteVerge uses a warm cream canvas and near-black warm ink. Coral is a scarce action color: it marks primary operations and essential emphasis, never the whole interface. Hierarchy comes from cream surface steps and 1dp hairlines before elevation or shadows. The real map remains unfiltered and is reused while switching between point and route modes.

## Principles

- Map first: route and point previews remain the visual anchor.
- Stable controls: the route action row keeps its two columns and dimensions in ready, running, and paused states.
- One action color: coral is reserved for primary actions, active state, and necessary emphasis.
- Whole-row interaction: saved records select from the complete row; menus and actions have independent targets.
- Semantic status: the Alipay NFC verification dot stays green; warning, error, and business-priority switches retain their own neutral/status colors.
- Low-noise motion: selector and content transitions are short and independent from map loading.

## Colors

Token | Value | Use
--- | --- | ---
`primary` | `#CC785C` | Main action buttons and essential emphasis
`primary-pressed` | `#A9583E` | Pressed/active primary action
`primary-disabled` | `#E6DFD8` | Disabled controls
`canvas` | `#FAF9F5` | Page background and secondary buttons
`surface-soft` | `#F5F0E8` | Light grouping surfaces
`surface-card` | `#EFE9DE` | Cards and unselected control groups
`surface-strong` | `#E8E0D2` | Selected rows, selected presets, active selector item
`ink` | `#141413` | Primary text and selected icons
`body` | `#3D3D3A` | Normal text
`muted` | `#6C6A64` | Metadata and labels
`muted-soft` | `#8E8B82` | Disabled/tertiary text
`hairline` | `#E6DFD8` | 1dp borders
`hairline-soft` | `#EBE6DF` | Subtle dividers
`surface-dark` | `#181715` | Dark emphasis surfaces
`success` | `#5DB872` | Verified/enabled status, including Alipay NFC dot
`warning` | `#D4A017` | Warning state
`error` | `#C64545` | Destructive/error state

Avoid cool blue brand accents, gradients, and decorative shadows. Shadows are 0dp by default; color-block contrast and hairlines establish depth.

## Typography

Use the Android system humanist sans-serif for readable native UI. Display serif is optional; do not add a font dependency solely for the title.

Token | Size | Weight | Use
--- | --- | --- | ---
`screen-title` | 32sp | 400 | App title
`section-title` | 18–22sp | 500–600 | Section headings
`body` | 16–17sp | 400 | Labels and actions
`body-strong` | 16–17sp | 600 | Selected names and key values
`caption` | 13–14sp | 400 | Coordinates, distances, metadata

Use tabular numerals for coordinates, distances, speeds, and counts where available.

## Layout and shapes

- Base spacing: 4 / 8 / 12 / 16 / 20 / 24 / 32dp.
- Screen horizontal padding: 16–24dp. On Home, the control area is fixed and only the saved-record viewport scrolls; all areas respect safe insets.
- Status card: warm soft surface, 16–20dp radius, 16dp internal padding, trailing badge that does not move the description.
- Mode selector: full-width warm segmented surface, 52dp minimum height. Selected item animates as one moving warm-color capsule, uses dark ink and a check icon; inactive item stays on the canvas. The two transparent option targets locally set `indication = null` with remembered interaction sources: holding an option must not draw a Ripple, rectangular pressed layer, second background, or elevation. Preserve tab semantics and focus handling; only the moving capsule expresses selection.
- Map preview: reused map instance, full available width, 220dp home preview, 20dp radius, 1dp hairline, no filter or heavy shadow.
- During a running route, the preview adds one semantic current-position marker derived from the persisted active route clock; the base map and route geometry remain unchanged.
- Point coordinates: keep the two-column layout with 56dp text-field height and compact vertical rhythm; never reduce type size to compress it.
- Route speed presets: keep all four controls in one row at a shared 56dp height; reduce container spacing rather than changing calculations or type scale.
- Primary buttons: 48dp minimum height, pill radius, coral fill, white label.
- Secondary buttons: same dimensions and stable placement, canvas fill, dark ink, 1dp hairline.

## Saved records

Point and route history use `SavedRecordRow` with the type-safe `PointRecord` and `RouteRecord` models. Both rows are 68dp high with the same horizontal padding (16dp), medium radius (12dp), hairline, selected surface, press feedback, vertical alignment, and trailing overflow target. Stable record IDs are used as Compose keys. Adjacent rows use a 4dp gap without collapsing into one surface. Each mode owns an independent `LazyListState`; switching modes preserves the other mode's record position.

- Point records show only the point name and coordinate subtitle. If metadata is unavailable, the subtitle line is reserved so the row does not shrink.
- Route records show route summary fields such as distance, point count, and loop mode.
- Tapping the row selects the record and updates the map/configuration. Tapping the overflow menu does not select the row.
- The former right-side triangle/play controls are removed. Route playback is available from the row overflow menu (`开始路线`) and the page-level primary action.

## Simulation states

State | Route action row | Point action
--- | --- | ---
Ready | `新建路线` (secondary) + `开始路线` (primary) | `地图选点` (secondary) + `开始定点` (primary)
Running | `暂停模拟` (secondary) + `停止模拟` (primary) | `停止模拟` only
Paused | `继续模拟` (secondary) + `停止模拟` (primary) | Not applicable

The two route columns keep their width, height, and gap in every state. Pausing retains the current route position; resuming never restarts at the beginning.

## Home scroll boundary and fixed NFC tool bar

Home is a bounded `Column` in the normal state, not a page-level `verticalScroll` or outer `LazyColumn`. The title/settings app bar, status card, selector, map preview, mode-specific configuration/actions, and the `保存点位` / `保存路线` heading remain outside the list. The matching saved-record `LazyColumn` receives the single `weight(1f)` remainder between that fixed content and the bottom bar, so only records can scroll and their drawing/touch bounds cannot overlap controls. While the IME is visible, the fixed content switches to a bounded vertical scroll viewport and the record list gets an explicit maximum height; this allows focused fields to be brought into view without ever compressing buttons.

The Alipay NFC row is the `Scaffold.bottomBar`, not a list item. Its opaque background is the page `canvas` token (`#FAF9F5` in light mode), with no card fill, rounded outer container, border, or shadow. It uses `navigationBarsPadding()` once when the IME is hidden. While the IME is visible, Home removes the bar from the hierarchy so it is not moved above the keyboard or included in resize calculations; it returns automatically when the IME closes. Scaffold measures the bar and reserves its inset only in the normal app-window state; no additional bottom padding is applied to the record list. Point and route modes share this same bottom bar.

The map remains a fixed-position embedded surface and keeps its own map gestures. It is 220dp when space permits and reduces to 176dp, 144dp, or 112dp for progressively compact height or large font scale; record rows and buttons are never clipped or made page-scrollable to compensate.

## Background simulation lifecycle

Simulation runs in the existing `MockLocationService` foreground service (`foregroundServiceType="location"`). It owns one coroutine `Job`, mock providers, WakeLock, notification channel, and stop action; Composables do not own the loop. Session configuration and active elapsed time are persisted in `mock_location_session` preferences while a simulation is live so the UI can reconstruct session details after Activity recreation. The service returns `START_NOT_STICKY` for every command and never redelivers an old start/pause/resume intent or rebuilds a simulation from preferences. `MockLocationStateStore` derives running/paused flags from the live process-local service runtime, while a persisted session without that runtime is stale and cleared on cold start. Ordinary backgrounding, lock/unlock, and external-app navigation leave the foreground service running; removing the app task stops it through `stopWithTask` and the service cleanup path. Stopping clears preferences, cancels the job, removes providers, releases WakeLock, and removes the foreground notification.

## Tools and accessibility

The Alipay NFC tool remains visible after saved records. Its green status dot communicates verified/enabled state and must not be recolored coral. All interactive targets are at least 48dp and expose content descriptions. State transitions use approximately 150–250ms motion and do not reorder layout.

## Implementation guidance

Keep tokens in `ui/theme/Theme.kt`, shared controls in `ui/components`, and business state in the existing Activity/AppRoot flow. Reuse the existing map controller and map instance across mode changes. Material 3 is the component foundation; explicit colors, shapes, borders, and zero tonal elevation keep the warm editorial hierarchy consistent in light and dark themes.
