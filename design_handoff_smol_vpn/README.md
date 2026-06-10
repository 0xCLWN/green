# Handoff: smol vpn — connected-layer redesign

## Overview
A redesign of a small VLESS VPN client for **Android**. The goal was to simplify the flow and add settings while keeping the app minimal. The defining idea: the **connected state is a layer that rises up and covers the home screen**, and while it is up the app's configuration is **locked** (you disconnect to change servers or open settings). This makes the "you can't safely change routing mid-tunnel" rule visible instead of confusing.

Core capabilities added vs. the old app:
- Tap-to-select active server + one big Connect
- A connected "session" layer with live stats
- Per-config editing (rename, edit URI, test, delete)
- Import via QR / clipboard / subscription / manual
- A dedicated Settings screen (auto-connect, kill switch, always-on, routing, etc.)
- A persistent split-tunnel status line ("3 apps tunneled" / "whole-phone VPN")

## About the Design Files
The files in this bundle are **design references created in HTML/React+Babel** — a clickable prototype showing the intended look and behavior. They are **not** production code to ship. The task is to **recreate these designs in the target codebase's environment** (e.g. native Android/Kotlin + Jetpack Compose, or whatever the real app uses) following its established patterns, navigation, and component library. The HTML/CSS values below are the source of truth for look-and-feel; the JSX is illustrative of structure and state only.

## Fidelity
**High-fidelity.** Final colors, typography, spacing, radii, and interactions are all specified. Recreate the UI faithfully using the codebase's own UI toolkit. The one caveat: it's mocked at a fixed phone size (400×840 logical px) inside a device frame — treat that frame as scaffolding, not part of the design.

## Design Tokens

### Colors
| Token | Hex | Use |
|---|---|---|
| `--bg` | `#0F1512` | App canvas (deep green-black) |
| `--surface` | `#19211E` | Cards, inputs, rows |
| `--surface2` | `#212B27` | Hover / secondary fills |
| `--surface3` | `#2A3631` | Toggle track |
| `--border` | `#2C3833` | Hairline borders |
| `--border2` | `#3A4843` | Stronger borders / radio rings |
| `--text` | `#ECF3EF` | Primary text |
| `--dim` | `#8B9A92` | Secondary text |
| `--dim2` | `#677570` | Section labels |
| `--accent` | `#57E08A` | Live/active only (selected server, Connect, links) |
| `--accent-press` | `#46c576` | Pressed accent |
| `--accent-soft` | `rgba(87,224,138,.14)` | Accent tint backgrounds |
| `--on-accent` | `#06200F` | Text/icons on accent fills |
| `--warn` | `#E8A24A` | Lock nudge |
| `--danger` | `#F0735A` | Delete |
| connected gradient | `#1C7E50` → `#0E5536` | Connected layer background |
| connected glow | `#9affc0` | Pulse dot, sparkline, test-ok |

Accent is themeable; alternate accents shipped as a tweak: blue `#5AB0F0`, violet `#A98BF0`, amber `#F0B44A` (each with matching press/soft/on/gradient/glow — see `ACCENTS` in `app.jsx`).

### Typography
- **UI font:** Space Grotesk (400/500/600/700)
- **Data font (mono):** Space Mono (400/700) — used for URIs, ping, timers, region meta, stat values
- Sizes: screen titles 19px/600; big server name 30px/700 (-0.6 letter-spacing); status headline 25px/600; stat value 25px/700 mono; body 14–16px; section labels 11px/600 uppercase, 1.6px letter-spacing; URI/meta 10–13px mono

### Spacing / shape
- Radii: `--r-lg` 22px (Connect, layer top), `--r-md` 16px (cards/inputs), `--r-sm` 12px, pills 20–40px
- Screen padding: 14px 18px 22px
- Card padding: ~13px 14px; gap between cards 9px
- Connect button: full-width, 17px padding, 17px/700 label
- Shadows: soft dark only — e.g. Connect `0 12px 30px -12px rgba(0,0,0,.7)`

## Screens / Views

### 1. Home (idle)
- **Purpose:** pick a server, see what's tunneled, connect.
- **Layout:** vertical flex, full height. Top app bar → status block (centered) → split-tunnel line → "SERVERS" label → scrollable server list (flex:1) → Connect button pinned at bottom.
- **App bar:** left = wordmark ("smol vpn" with a 26px rounded accent-soft tile holding a shield glyph); right = 42px gear icon button (border `--border`, radius 13px).
- **Status block:** 96px circle (radial dark fill, dashed outer ring) holding a dim shield → headline "Not connected" → sub "Pick a server and tap connect to secure this device."
- **Split-tunnel line:** full-width `--surface` pill, 30px accent-soft icon tile (split glyph) + text "Split tunnel · **3 apps** tunneled" + "manage ›" in accent. Tapping toggles to "Whole-phone VPN · **all apps**".
- **Server card:** row = 21px radio (accent dot when selected) + name (15.5px/600, flag emoji prefix) over mono region meta + ping pill (mono; `.good` = accent tint when <60ms; "— ms" if untested) + 30px ⋯ button. Selected card: accent border + accent-soft gradient wash.
- **Add card:** dashed border, centered "+ Add server", accent border on hover.
- **Connect:** accent fill, `--on-accent` text, power icon + "Connect".

### 2. Connected layer (rises over Home)
- **Purpose:** the active session — status, live stats, disconnect.
- **Layout:** absolutely positioned, `inset:0`, `z-index:20`, covers Home. Slides via `transform: translateY(100%)` → `0`, transition `.55s cubic-bezier(.16,1,.3,1)`. Green gradient bg + top radial accent glow.
- **Top:** grab handle (centered) → row: "● Connected · secure" (pulsing glow dot) on left, "🔒 settings locked" chip on right.
- **Identity:** big server name 30px/700 + mono meta "🇩🇪 frankfurt · de · vless · reality".
- **Split line:** read-only variant (white-alpha fill).
- **Stats grid:** 2-col. Boxes for Ping, Uptime (live mm:ss), Download, Upload, and a full-width Throughput box with right-aligned session totals + a live sparkline (10 bars, animate height every ~1.1s).
- **Actions (bottom):** "⚡ Test connection" (re-checks route → "route healthy · 41 ms"), then outline "Disconnect".
- **Lock behavior:** tapping the "settings locked" chip fires a toast "Disconnect to change settings" + shakes the layer.

### 3. Settings (pushed screen, reachable only while disconnected)
- **Layout:** header (back + "Settings") → scroll body of grouped rows. Rows are joined cards (first/last rounded), label + optional sub-label on left, control on right.
- **Sections:** Connection (Auto-connect, Kill switch, Always-on — toggles), Routing (Split tunneling → "3 apps", DNS → "Automatic", Protocol → "VLESS · TCP"), Data (Import / subscriptions →), General (Connection notifications toggle, App version 1.2.0).
- **Toggle:** 46×27 track, `--surface3` off / accent-soft on, knob `--dim`→accent, slides 21px.

### 4. Edit server (pushed screen)
- Header back + "Edit server" + accent "Save" button.
- Fields: Name (text input), VLESS URI (mono textarea, 4 rows), Reachability ("Test connection" → spinner → "route ok · 41 ms"). Footer: full-width danger "Delete server".
- Inputs: `--surface` fill, `--border`, focus border = accent.

### 5. Import / Add server (pushed screen)
- Header back + "Add a server".
- Large QR placeholder (7×7 grid in a `--surface` card), label "or add another way", then 4 option rows (44px accent-soft icon tile + title + sub + chevron): Scan QR code, Paste from clipboard ("vless:// link detected"), Subscription link ("a group that auto-updates"), Enter manually.

## Interactions & Behavior
- **Connect:** `connected=true` → layer translateY 0 (.55s). **Disconnect:** `false` → translateY 100%.
- **Select server:** tap card sets active; Connect uses it.
- **Pushed screens:** slide in from right, `transform: translateX(100%)→0`, `.35s cubic-bezier(.3,0,.2,1)`, `z-index:30` (above the layer).
- **Lock model:** Settings/Edit/Import are only entered while disconnected. While connected, the layer covers Home so those entry points aren't reachable; the layer's lock chip explains why (toast + shake).
- **Split-tunnel line:** shows count only ("3 apps tunneled") or "Whole-phone VPN · all apps" when 0; never lists app names.
- **Toast:** bottom-center pill, fades up, auto-hides ~2.1s; warn variant has a lock icon.

## State Management
- `connected: bool` — drives the layer.
- `selId: string` — active server id.
- `servers: [{id,name,flag,region,ping,uri}]`
- `tunneled: number` — split-tunnel app count (0 = whole-phone).
- `view: 'settings' | 'edit' | 'import' | null` — current pushed screen.
- `editId` — server being edited.
- `settings: {autoConnect, killSwitch, alwaysOn, notify}`
- Live in the layer: uptime seconds (1s tick), sparkline bars (~1.1s tick), test status.
- Tweaks (optional, prototype-only): `accent`, `lockWhenConnected`, `showGraph`.

## Open product decisions (confirm before building)
1. **Lock settings while connected** — default ON (full lock). The prototype's `lockWhenConnected=false` mode turns the layer's lock chip into a working gear that opens Settings *over* the layer (hybrid). Pick one.
2. **Split-tunnel "manage ›"** is not yet a real screen — needs an app-picker if you want it functional.

## Assets
- **Icons:** all inline stroke SVGs, see `icons.jsx` (gear, shield, chevron, dots, plus, split, bolt, power, QR, clipboard, link, pencil, trash, arrows, lock). Re-draw with the codebase's icon set.
- **Flags:** emoji placeholders (🇩🇪 🇳🇱 🇯🇵) — replace with the app's flag assets if desired.
- **Fonts:** Space Grotesk + Space Mono (Google Fonts).
- No raster image assets.

## Files (in this bundle)
- `index.html` — entry; loads fonts, React+Babel, frame, then the scripts. Includes scale-to-fit.
- `styles.css` — **all design tokens + component styles (source of truth for look-and-feel).**
- `app.jsx` — shell, Home, ConnectedLayer, state machine, accent tweak map.
- `panels.jsx` — Settings, EditServer, Import pushed screens + Toggle.
- `icons.jsx` — inline SVG icon set.
- `tweaks-panel.jsx` — prototype-only tweak panel (not part of the product).
- `android-frame.jsx` — device bezel scaffolding (not part of the product).

To preview: open `index.html` in a browser. Toolbar → Tweaks toggles accent/lock/graph.
