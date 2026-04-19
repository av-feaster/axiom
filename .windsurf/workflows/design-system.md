---
description: Follow Lumina Neural Design System for UI development
---

# Lumina Neural Design System Guidelines

When working on UI components in the axiom-android-sdk, always follow the Lumina Neural Design System defined in `docs/design-system.md`.

## Key Principles

1. **Atmospheric Precision:** High contrast, dark backgrounds with vibrant, luminous accents
2. **Structural Clarity:** Clear hierarchies using Space Grotesk for headings and Inter for UI controls
3. **Tactile Feedback:** Subtle glows, glassmorphism (backdrop blurs), and smooth transitions

## Color Usage

Always use colors from the design system via `AxiomTheme.colors`:

- `primary` (#6366F1 Indigo) - Primary actions and progress
- `accentLuminous` (#c0c1ff) - Text highlights and icons
- `background` (#13131b) - Primary surface
- `backgroundSecondary` (#1b1b23) - Cards and elevated sections
- `success` (#10b981 Emerald) - Success states
- `error` (#f43f5e Rose) - Error states

**Never hardcode color values.** Always use the design system colors.

## Typography

- Headings: Space Grotesk (Bold/Medium) - For technical data and high-impact text
- Body & UI: Inter - For readability and clarity

Use appropriate text styles from `AxiomTheme.typography`:
- `displayLarge` - 30sp, Bold
- `displayMedium` - 24sp, Medium
- `title` - 20sp, Medium
- `bodyLarge` - 16sp, Normal
- `bodyMedium` - 14sp, Normal
- `bodySmall` - 12sp, Normal
- `caption` - 10sp, Normal
- `label` - 10sp, Normal

## Shapes & Elevation

- Corner Radius: 8-12px (use `AxiomTheme.shapes.small` or `AxiomTheme.shapes.medium`)
- Shadows: Soft, deep shadows with indigo tints

## Component Guidelines

### Bottom Sheet Shell
- 32px top corner radius
- Prominent drag handle
- Semi-transparent backdrop blur
- Integrated BottomNavBar with active state gradients

### Model Cards
- Content-first design
- Clear typography for model stats (parameters, speed, format)
- Hover states with subtle scale changes and border-color shifts

### Download Indicators
- High-contrast gradients from indigo to lavender
- Slight outer glow to indicate activity
- Clearly defined action buttons using tonal layering

## Implementation Checklist

Before implementing any UI component:

1. Check `docs/design-system.md` for the latest specifications
2. Use `AxiomTheme.colors` for all colors
3. Use `AxiomTheme.typography` for all text styles
4. Use `AxiomTheme.shapes` for all corner radii
5. Ensure the component follows the Lumina Neural aesthetic
6. Test for high contrast and accessibility
