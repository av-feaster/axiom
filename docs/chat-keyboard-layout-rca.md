# Chat keyboard / empty space — RCA and fix attempt log

This document tracks **root cause analysis**, **instrumentation**, and **failed/partial attempts** for the bug where opening the soft keyboard in **ChatScreen** (hosted inside **Material3 `BottomSheetScaffold`**) leaves a **large empty region** between the composer (“Reset Context” + text field) and the keyboard. Tools (Cursor, Windsurf) struggled because several distinct failure modes look similar in screenshots.

---

## Symptom (user-visible)

- With the keyboard open, the **message composer appears high** on the sheet (directly under the app header area in severe cases).
- A **tall blank band** sits between the composer and **Gboard** (or any IME).
- **Chat history** may appear missing or squashed—not always “keyboard overlap”.

Screenshot reference (workspace):  
`assets/image-ffedf3f9-84ea-4322-a4ce-95016864f110.png`

---

## How to capture evidence (height / inset logs)

### Logcat filter

```text
adb logcat -s ChatLayout:I ChatScreen:D
```

### Tags

| Tag          | Purpose |
|-------------|---------|
| **`ChatLayout`** | Throttled layout + constraint snapshot (see `ChatLayoutLog.kt` + `ChatScreen.kt`). |
| **`ChatScreen`** | Existing generation/debug logs (unchanged). |

### What gets printed (when `CHAT_LAYOUT_DEBUG = true` in `ChatScreen.kt`)

1. **Line A — constraints + insets (on change)**  
   - `hasBoundedH` — if `false`, **`Modifier.weight(1f)` on `LazyColumn` is not applied** (see RCA below).  
   - `maxH` / `maxW` — parent constraints in **px**.  
   - `columnHeightDp` — height we clamp the root `Column` to.  
   - `imeBottomPx`, `navBottomPx` — `WindowInsets` bottom values.  
   - `screenHeightDp` — configuration fallback.

2. **Line B — throttled ~450ms** — global positions (window Y, height):  
   - `ChatColumn` — root chat column after insets.  
   - `LazyColumn` — message list area.  
   - `InputColumn` — composer block.

**Paste those lines** into an issue or follow-up; they distinguish “unbounded height” vs “double IME inset” vs “sheet not resizing”.

### Turning debug off

In `ChatScreen.kt`:

```kotlin
private const val CHAT_LAYOUT_DEBUG = false
```

---

## Root cause analysis (ordered hypotheses)

### H1 — **`weight(1f)` ineffective under unbounded max height** (primary, matches screenshot)

**Mechanism:** In a `Column`, `Modifier.weight(1f)` only distributes **remaining** vertical space when the `Column` has a **bounded** maximum height. If the parent (often **`BottomSheetScaffold` `sheetContent`**) passes **`constraints.hasBoundedHeight == false`** (infinite or undefined max height), Compose **ignores `weight`**. The `LazyColumn` then sizes to its **minimum** (often tiny when the list is empty). The next sibling (**input `Column`**) is placed **immediately under** the `TopAppBar`, leaving the rest of the window **visually empty** down to the keyboard.

**Evidence to collect:** Log line shows `hasBoundedH=false` and **`LazyColumn` height very small** vs `ChatColumn` height large.

**Mitigation in code:** Wrap chat UI in **`BoxWithConstraints`**, compute `columnHeightDp = if (constraints.hasBoundedHeight) constraints.maxHeight.toDp() else configuration.screenHeightDp.dp`, and set root **`Column(Modifier.height(columnHeightDp))`** so `weight(1f)` always has a finite cap.

**Risk:** Fallback `screenHeightDp` may exceed the **actual** sheet height if constraints stay unbounded but the visible sheet is shorter (rare; document if seen).

---

### H2 — **Double application of IME insets** (secondary; “strip” above keyboard)

**Mechanism:** Host uses **`WindowCompat.setDecorFitsSystemWindows(window, false)`** (edge-to-edge). The window may **already** shrink or offset for IME while Compose still applies **`imePadding()`** / **`windowInsetsPadding(IME)`**, reserving **another** full keyboard height → empty band.

**Evidence:** `imeBottomPx` large **and** `ChatColumn` bottom already sitting above keyboard by ~one keyboard height without `weight` bug.

**Past attempts:** `imePadding()` only on input; then `navigationBars.union(ime)` only on root.

---

### H3 — **IME insets not reaching sheet content** (tertiary)

**Mechanism:** IME `WindowInsets` can read **0** inside some nested surfaces; then padding does nothing and the keyboard draws over content (different symptom unless combined with H1).

**Evidence:** Keyboard open but `imeBottomPx == 0` consistently.

---

### H4 — **`BottomSheetScaffold` + `fillMaxHeight()` on sheet `Column`**

**Mechanism:** [`AxiomBottomSheet.kt`](../axiom-android-sdk/src/main/java/com/axiom/android/sdk/ui/AxiomBottomSheet.kt) uses a sheet `Column` with **`fillMaxHeight()`**. Interaction with scaffold measurement can contribute to odd constraints (related to H1).

**Next experiment if H1 insufficient:** Replace with **`fillMaxSize()`** or cap sheet `Column` with **`heightIn(max = …)`** from configuration and re-log constraints.

---

## Fix attempt log (chronological)

| Try | Change | Result / notes |
|-----|--------|----------------|
| 1 | `imePadding()` + `navigationBarsPadding()` on **input** `Column` only | Could still show gap if window already insets for IME. |
| 2 | Single **`windowInsetsPadding(IME ∪ navigationBars)`** on **ChatScreen** root; remove inner IME padding | Reduces double-inset (H2); **did not fix** layout if H1 dominates. |
| 3 | **RCA doc + `ChatLayout` logs** + **`BoxWithConstraints` + explicit `Column` height** when `hasBoundedHeight` is false | Addresses H1; **verify** with Logcat (`hasBoundedH`, `LazyColumn` height). |
| 4 | **`adjustResize`** on sample `MainActivity` + root **`navigationBarsPadding` only** + **`imePadding()`** on composer only + **`fillMaxHeight()`** when constraints bounded | Targets **`handleResized abandoned`** + full-height `maxH` with growing `imeBottomPx` (log pattern above). |

---

## Files touched (Try 3–4)

| File | Role |
|------|------|
| [`ChatScreen.kt`](../axiom-android-sdk/src/main/java/com/axiom/android/sdk/ui/screens/ChatScreen.kt) | `BoxWithConstraints`, bounded `fillMaxHeight`, nav on root, `imePadding` on composer, `CHAT_LAYOUT_DEBUG`, layout hooks. |
| [`ChatLayoutLog.kt`](../axiom-android-sdk/src/main/java/com/axiom/android/sdk/ui/screens/ChatLayoutLog.kt) | Throttled `ChatLayout` logging helper. |
| [`AndroidManifest.xml`](../sample/src/main/AndroidManifest.xml) (sample) | `android:windowSoftInputMode="adjustResize"` on `MainActivity` (Try 4). |
| This doc | RCA + try log + how to read logs. |

---

## Log interpretation (real device capture)

Example (`com.axiom.sample`):

- `VRI[MainActivity] handleResized abandoned!` — the window **did not** apply the usual resize for the IME; `constraints.maxHeight` can stay at **full display** (e.g. `2172px`) while `imeBottomPx` still ramps to **~897px**.
- **`columnHeightDp=724`** with **`maxH=2172`** — bounded height is **full screen** in px; root **`windowInsetsPadding(IME ∪ nav)`** then reserves **~897px bottom** inside that full height → large **blank band** above the keyboard (IME strip + non-resizing window).

**Mitigation (Try 4):** Sample `MainActivity` **`android:windowSoftInputMode="adjustResize"`** so `maxHeight` tends to **shrink** when the IME is shown. Chat UI: **nav bars on root only**, **`imePadding()` on the composer `Column` only** so the message list is not carved out by the full IME inset twice.

---

## Next steps if the bug persists

1. Paste **full `ChatLayout` lines** with keyboard closed vs open (before/after focus on `OutlinedTextField`).
2. If `handleResized abandoned` still appears after Try 4, capture **OEM / API level**; some devices need **`SOFT_INPUT_ADJUST_NOTHING`** + full manual insets (last resort).
3. Consider **`ModalBottomSheet`** or **`enableEdgeToEdge()`** + official M3 IME patterns for sheets (API-level dependent).
4. If **`LazyColumn` height is large** but still blank, suspect **drawing / clip** on the sheet surface—not measurement.

---

## References (internal)

- [`ChatScreen.kt`](../axiom-android-sdk/src/main/java/com/axiom/android/sdk/ui/screens/ChatScreen.kt)  
- [`AxiomBottomSheet.kt`](../axiom-android-sdk/src/main/java/com/axiom/android/sdk/ui/AxiomBottomSheet.kt)  
- [`MainActivity.kt`](../sample/src/main/java/com/axiom/sample/MainActivity.kt) (`setDecorFitsSystemWindows(false)`)  

---

*Last updated: documents Try 1–3 and adds `ChatLayout` instrumentation.*
