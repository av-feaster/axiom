# Design System: Lumina Neural
**Creative North Star: "The Synthetic Atelier"**

## 1. Vision & Core Principles
Lumina Neural is designed for the high-performance world of AI model management. It moves away from generic utilitarianism toward a "Synthetic Atelier" aesthetic—precise, technical, yet deeply elegant.

*   **Atmospheric Precision:** High contrast, dark backgrounds with vibrant, luminous accents.
*   **Structural Clarity:** Clear hierarchies using "Space Grotesk" to denote technical data and "Inter" for UI controls.
*   **Tactile Feedback:** Subtle glows, glassmorphism (backdrop blurs), and smooth transitions to give the interface a premium, responsive feel.

## 2. Visual Foundation

### Color Palette
*   **Surface (Primary):** `#13131b` — A deep, midnight navy that serves as the canvas.
*   **Surface (Secondary):** `#1b1b23` — Used for cards and elevated sections to create depth.
*   **Accent (Primary):** `#6366F1` (Indigo) — The signature brand color for primary actions and progress.
*   **Accent (Luminous):** `#c0c1ff` — A desaturated lavender used for text highlights and icons.
*   **Status Colors:**
    *   Success: `#10b981` (Emerald)
    *   Warning/Error: `#f43f5e` (Rose)

### Typography
*   **Headings:** *Space Grotesk* (Bold/Medium) — Technical, modern, and high-impact.
*   **Body & UI:** *Inter* — Optimized for readability and clarity in dense data environments.

### Shape & Elevation
*   **Corner Radius:** `ROUND_EIGHT` (8px to 12px) for a balanced, modern look.
*   **Shadows:** Soft, deep shadows with indigo tints (`shadow-[0_40px_40px_rgba(99,102,241,0.06)]`) to simulate depth without clutter.

## 3. Core Components

### Bottom Sheet Shell
*   **Styling:** 32px top corner radius, prominent drag handle, and a semi-transparent backdrop blur.
*   **Navigation:** Integrated `BottomNavBar` with active state gradients.

### Model Cards
*   **Layout:** Content-first design with clear typography for model stats (parameters, speed, format).
*   **Interactions:** Hover states feature subtle scale changes and border-color shifts.

### Download Indicators
*   **Progress Bars:** High-contrast gradients from indigo to lavender with a slight outer glow to indicate activity.
*   **Action States:** Clearly defined "Pause," "Cancel," and "Install" buttons using tonal layering.
