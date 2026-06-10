# smol vpn — Orbit logo

The **Orbit** mark: a small lit core inside a ring with a single accent arc — a tiny
satellite of protection. Minimal, calm, and it scales cleanly to a 16px favicon.

## Files
| File | Use |
|---|---|
| `orbit-mark.svg` | The bare mark (transparent bg, dark UI) — app bar, favicons, inline. |
| `orbit-mark-light.svg` | Same mark tuned for light backgrounds. |
| `orbit-app-icon.svg` | 512×512 rounded-tile app icon with glow + aura. Export to PNG for stores. |
| `orbit-wordmark.svg` | Horizontal lockup "● smol vpn" for dark headers. |
| `orbit-wordmark-light.svg` | Wordmark for light backgrounds. |

## Colors
- Accent (dark bg): `#57E08A` · core + arc
- Accent (light bg): `#1F9E5E` · core + arc
- Ring (dark): `#2C3833` · (light): `#C3CDC7`
- App-icon tile: gradient `#1F2B25 → #0C120F`, radius 115/512

## Geometry (for redrawing natively)
- viewBox `0 0 100 100`; ring `r=34`, stroke-width `6`; core `r=9`.
- Accent arc = ring stroke with `stroke-dasharray="56 220"`, `stroke-linecap="round"`,
  rotated `-90°` about center (arc opens from top, sweeps ~95°).

## Fonts (wordmark)
- "smol" — Space Grotesk 700, letter-spacing −1.5
- "vpn" — Space Mono 400, letter-spacing +1, color = dim
- The wordmark SVGs keep text live (editable). Outline the text in your design tool
  if you need a font-independent asset.
