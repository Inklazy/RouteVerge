# RouteVerge Design System

## 0. Purpose

This document defines the visual language, interaction patterns, layout rules, and UI implementation constraints for RouteVerge.

RouteVerge is an Android utility application for:

* local location simulation
* route planning
* route persistence
* route playback
* map-based interaction
* testing and debugging of Android location features

The product should feel like a serious, polished utility application rather than an experimental developer tool.

The design language must be:

* modern
* calm
* precise
* functional
* map-first when working with routes
* information-first on data screens
* native to Android
* visually consistent
* restrained rather than decorative

Do not optimize for visual novelty.

Optimize for:

> clarity → hierarchy → usability → consistency → polish

---

# 1. Design Direction

## 1.1 Primary design reference

The primary visual language is a custom Android utility design built on Material 3.

The design draws inspiration from:

### Google Maps

Use Google Maps as the primary reference for:

* map-first layouts
* floating controls
* route visualization
* current-location presentation
* bottom-sheet interaction
* map overlay hierarchy

### Uber

Use Uber as a reference for:

* strong information hierarchy
* high-contrast primary actions
* map + action relationships
* minimal chrome
* efficient interaction flows
* restrained use of color

Do not copy Uber branding.

Do not use Uber's proprietary typography or logo.

### Linear

Use Linear as a reference for:

* information density
* typography hierarchy
* status presentation
* compact secondary information
* clean list structures
* restrained borders
* minimal visual noise

### Miuix

Miuix is a secondary reference only.

Absorb selected ideas such as:

* comfortable preference rows
* compact settings layouts
* soft surfaces
* clear control states
* Android utility-app ergonomics

Do NOT transform the entire application into a MIUI clone.

---

# 2. Product Personality

RouteVerge should feel:

* trustworthy
* technically capable
* quiet
* focused
* modern
* deliberate

It should NOT feel:

* playful
* gamified
* corporate-dashboard-like
* overly futuristic
* neon
* overly glassy
* excessively rounded
* cluttered
* like a generic AI-generated UI

The interface should communicate:

> "This is a reliable tool that happens to look good."

---

# 3. Platform Foundation

Use:

* Jetpack Compose
* Material 3
* Android platform conventions
* Material 3 components wherever possible

Do not replace Material 3 with a third-party component library unless there is a concrete technical reason.

Do not introduce Miuix as a second full component system.

The project should maintain one primary component language.

Material 3 is the implementation foundation.

Custom RouteVerge components should wrap or extend Material 3 rather than recreate its behavior unnecessarily.

---

# 4. Color System

## 4.1 Brand Color

Primary brand color:

`#3482FF`

This is the existing RouteVerge brand blue and should remain the primary identity color unless there is a strong reason to change it.

Use it for:

* primary actions
* active navigation
* selected states
* route-related emphasis
* interactive links
* important status indicators
* focused controls

Do NOT use the brand blue everywhere.

Blue should communicate:

> interaction / selection / active state / primary action

---

## 4.2 Light palette

Reference foundation:

* Primary: `#3482FF`
* Primary Soft: `#EAF2FF`
* Background: `#F4F6FB`
* Surface: `#FFFFFF`
* Surface Elevated: `#FDFEFF`
* Surface Container: `#F0F3F9`
* Border: `#DDE3EE`
* Primary Text: `#111827`
* Secondary Text: `#667085`
* Disabled Text: `#98A2B3`
* Success: `#36D167`
* Warning: `#FFA726`
* Error: `#FF4D4F`

These values are the current RouteVerge foundation.

Do not introduce a new arbitrary color for individual screens.

---

# 5. Dark Theme

Dark theme should not simply invert the light theme.

Use layered dark surfaces.

Reference foundation:

* Background: `#101318`
* Surface: `#161A21`
* Surface Variant: `#303640`
* Primary: `#9DC3FF`
* Primary Container: `#124B86`
* Text: `#E1E4EA`
* Secondary Text: `#C4CAD4`
* Border: `#474F5C`
* Success: `#77DD96`
* Error: `#FFB4AB`

Dark mode should feel like a professional tool.

Avoid pure black backgrounds except where the underlying map or a specific media surface requires it.

---

# 6. Semantic Color Rules

Color must have meaning.

### Blue

Primary action / selected / interactive / route emphasis.

### Green

Success / ready / completed / healthy state.

### Orange

Warning / attention / partially available.

### Red

Error / destructive / dangerous / failed state.

### Gray

Neutral / disabled / inactive / secondary information.

Do not use color merely for decoration.

Never create a new accent color simply because a card "looks empty."

---

# 7. Typography

Use the Android system font.

Do not add a custom font unless explicitly required.

Typography should prioritize:

1. readability
2. hierarchy
3. compactness
4. consistency

Recommended semantic hierarchy:

### Screen Title

Large, confident, but not oversized.

Use for the main page title.

### Section Title

Medium-to-large semibold.

Used to separate major sections.

### Primary Body

Regular weight.

Used for important readable content.

### Secondary Body

Slightly smaller or lower contrast.

Used for explanations and supporting information.

### Label

Medium/semibold.

Used for control labels, chips and compact status information.

### Caption

Small and muted.

Used for metadata and low-priority information.

Avoid using bold everywhere.

Do not solve hierarchy by making everything larger and heavier.

---

# 8. Spacing

Use an 8dp-oriented spacing system.

Preferred values:

* 4dp
* 8dp
* 12dp
* 16dp
* 20dp
* 24dp
* 32dp

Use 4dp only for tight internal relationships.

Use 8–16dp for normal component spacing.

Use 20–24dp for major sections.

Use 32dp for major visual separation.

Do not create arbitrary spacing values without a strong visual reason.

---

# 9. Corner Radius

Use a small, controlled radius vocabulary.

Recommended:

* 12dp — small controls
* 16dp — standard controls/cards
* 22dp — prominent controls
* 28dp — large surfaces
* 999dp — pill

The existing RouteVerge shape system can be normalized around this scale.

Avoid random values such as:

* 17dp
* 19dp
* 21dp
* 23dp
* 27dp

unless there is a documented reason.

---

# 10. Surface Hierarchy

The app should not be made from dozens of floating cards.

Preferred hierarchy:

1. Background
2. Surface
3. Elevated surface
4. Floating control

A surface should exist only when it improves grouping or interaction.

Do not place every item inside a Card.

Lists and settings should usually use rows rather than individual cards.

---

# 11. Cards

Cards are for meaningful grouping.

Good uses:

* current route state
* route summary
* important information block
* preview
* grouped settings
* major actions

Bad uses:

* every single setting
* every text label
* every list item
* decorative containers

Avoid "cardception" where a card contains many smaller cards.

---

# 12. Borders and Elevation

Prefer:

* surface color difference
* subtle borders
* spacing

over heavy shadows.

Shadows should be rare.

Use elevation mainly for:

* floating controls
* dialogs
* sheets
* important overlays

Do not give every card a shadow.

The app should feel flat and modern.

---

# 13. Primary Action

Each screen should have one obvious primary action where appropriate.

Primary actions should use:

* filled Material 3 button
* clear contrast
* concise label
* optional icon if useful

Do not make every action a filled blue button.

Secondary actions should have lower visual weight.

---

# 14. Buttons

Preferred button hierarchy:

### Filled

Primary action.

### Tonal

Important secondary action.

### Outlined

Secondary alternative.

### Text

Low-priority action.

### Icon Button

Compact contextual action.

Never have multiple competing primary buttons in the same visual area unless there is a clear workflow reason.

---

# 15. Chips and Status Badges

Use chips/status badges for short states only.

Examples:

* Ready
* Running
* Paused
* Completed
* Error
* Unsaved
* Active

Status colors must follow the semantic color system.

Do not use badges as decorative labels.

---

# 16. Lists

Lists should feel lightweight.

Preferred structure:

```text
Title
Secondary information
Optional trailing control
```

Do not turn every row into a large rounded container.

Settings should generally use grouped rows.

---

# 17. Settings

Settings should be one of the places where the Miuix influence is most visible.

Use:

* grouped sections
* clear section labels
* comfortable row heights
* simple trailing controls
* subtle separators or surface grouping

Typical row:

```text
Icon / optional
Title
Description / optional
Trailing control
```

Do not use a giant card for every setting.

---

# 18. Navigation

Navigation should follow Material 3 conventions.

Use NavigationBar when the application has several major top-level destinations.

The active destination should be obvious but not visually aggressive.

Do not create overly large custom navigation controls unless there is a specific product reason.

---

# 19. Maps

Map screens are special.

The map is the primary canvas.

Do not cover the map with unnecessary cards.

Preferred hierarchy:

```text
MAP
 ├── small floating utility controls
 ├── current location
 ├── route visualization
 └── compact bottom action surface
```

The map should remain visible.

Controls should float above it rather than consume most of the screen.

---

# 20. Route Visualization

Routes should be visually prominent.

The primary route should have strong contrast against the map.

Secondary route information should be quieter.

Use:

* clear route stroke
* start marker
* destination marker
* current position
* optional selected segment

Avoid excessive marker decoration.

---

# 21. Bottom Sheets

Bottom sheets are preferred when contextual information needs to coexist with the map.

Good uses:

* route details
* route controls
* playback controls
* selected location details
* action confirmation

Sheets should not become giant full-screen forms unless the workflow genuinely requires it.

Use Material 3 sheet behavior.

---

# 22. Floating Controls

Floating buttons should:

* have a clear purpose
* be easy to reach
* avoid covering important map information
* use familiar icons
* provide content descriptions

Do not place multiple floating buttons where they visually merge together.

---

# 23. Empty States

Every major collection or data screen needs a deliberate empty state.

It should explain:

1. what is empty
2. why it matters
3. what the user can do next

Avoid generic:

> "No data."

Prefer:

> "No saved routes yet."

with a clear next action.

---

# 24. Loading States

Avoid making the whole app spin when only one part is loading.

Prefer:

* local progress indicators
* skeletons
* inline state changes

Use indeterminate progress only when the duration is genuinely unknown.

---

# 25. Error States

Errors should be:

* clear
* localized
* actionable
* calm

Show:

* what went wrong
* whether the current data is safe
* what the user can do

Do not use giant red warning cards for ordinary errors.

---

# 26. Success Feedback

Success should be lightweight.

Prefer:

* Snackbar
* small status change
* icon transition
* subtle animation

Avoid giant success dialogs unless the operation is important enough to require confirmation.

---

# 27. Dialogs

Use dialogs only for decisions that require interruption.

Examples:

* delete route
* clear data
* destructive operation
* critical confirmation

Do not use dialogs for ordinary informational messages.

---

# 28. Motion

Motion should be subtle and functional.

Use animation for:

* navigation
* expanding/collapsing
* state transition
* route playback
* sheet interaction
* button state changes

Avoid:

* constant bouncing
* excessive scaling
* decorative animations
* long transitions

Animation should reinforce hierarchy.

---

# 29. Touch Targets

Interactive targets should be comfortably tappable.

Prefer at least 48dp touch targets where practical.

Icons may visually appear smaller but should retain adequate hit area.

Do not create tiny icon-only controls.

---

# 30. Accessibility

All interactive controls must have meaningful semantics.

Check:

* content descriptions
* contrast
* touch target size
* disabled state
* text scaling
* TalkBack compatibility

Never encode essential meaning using color alone.

---

# 31. Responsive Behavior

The UI must remain usable on:

* compact phones
* normal phones
* large phones
* landscape
* larger font scales

Do not hardcode screen dimensions.

Do not assume a single phone aspect ratio.

Use adaptive Compose layouts where appropriate.

---

# 32. Visual Density

The target density is:

> medium density

Not:

* extremely spacious
* dashboard dense

Normal pages should have breathing room.

Data-heavy screens may be denser.

Settings may be compact.

Map screens should prioritize map visibility.

---

# 33. Iconography

Use Material Symbols / Android-compatible icons.

Icons should have consistent visual weight.

Do not mix:

* filled cartoon icons
* thin outline icons
* emoji
* random third-party icon styles

within the same component hierarchy.

---

# 34. Dark Mode Rules

Dark mode should preserve hierarchy.

Do not simply replace:

`white → black`

Maintain:

* distinct surface levels
* readable secondary text
* visible borders
* clear active state
* controlled semantic colors

Maps may use their own map rendering theme independent of app surfaces.

---

# 35. Screens

## Home

The home screen should answer:

> What is the current state and what can I do next?

Prioritize:

1. current status
2. primary action
3. useful route information
4. recent or relevant data
5. secondary utilities

Avoid excessive cards.

---

## Route

Route creation/editing should make the route itself the main subject.

Separate:

* route canvas
* route controls
* route metadata
* destructive actions

---

## Playback / Running State

The running state should emphasize:

* current status
* current location
* route
* playback progress
* primary control

During active playback, reduce visual noise.

---

## History / Saved Data

Use efficient list presentation.

Prioritize:

* route name
* date/time
* distance or important metric
* state
* quick action

Avoid large decorative cards for every item.

---

## Settings

Use grouped preference rows.

Keep secondary descriptions muted.

Use Material controls.

---

# 36. Do

* use Material 3 as the foundation
* use a small number of spacing values
* use a small number of radius values
* prioritize hierarchy
* keep maps visible
* reuse components
* use semantic colors
* make primary actions obvious
* use subtle surfaces
* test dark mode
* test small screens
* verify screenshots after UI changes

---

# 37. Do Not

* do not make every section a card
* do not use random colors
* do not use random corner radii
* do not add gradients without a product reason
* do not overuse blur
* do not use giant shadows
* do not make every button bright blue
* do not create dozens of one-off components
* do not copy Apple, Uber, Google Maps or Miuix literally
* do not introduce Miuix as a second competing design system
* do not sacrifice usability for aesthetics
* do not make the UI look like a generic AI dashboard

---

# 38. Architecture Guidance

UI should be separated from business logic.

Recommended structure:

```text
ui/
├── theme/
├── components/
├── navigation/
└── screens/
    ├── home/
    ├── route/
    ├── playback/
    ├── history/
    └── settings/
```

Reusable UI belongs in `components`.

Screen-specific composition belongs in the relevant screen package.

Business logic must not be duplicated inside Composables.

---

# 39. Design Decision Rule

When uncertain, prefer:

1. Material 3 convention
2. existing RouteVerge design tokens
3. this DESIGN.md
4. simple and clear solution

Do not invent a visually novel solution simply because one is possible.

---

# 40. Final Product Test

A successful redesign should make RouteVerge look like:

> a polished, modern Android utility app with a map-first interaction model and a strong but restrained visual identity.

It should NOT look like:

> an AI-generated collection of fancy cards.

The final UI should feel intentional even when the user never notices the design system itself.
