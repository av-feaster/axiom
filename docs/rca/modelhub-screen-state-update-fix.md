# RCA: ModelHubScreen Not Updating on Download Start

## Issue Description
When clicking "Add to App" on the ModelHubScreen, the card did not change to show download progress. The UI remained static showing the same "Add to App" button instead of transitioning to the DownloadProgressCard or showing the downloaded state.

## Symptoms
- User clicks "Add to App" button
- Download starts in background (visible in logs)
- UI card remains unchanged - still shows "Add to App" button
- No visual feedback that download has started
- Registry being fetched repeatedly every second

## Root Cause Analysis

### Primary Issue: Improper State Flow Observation
**Location:** `ModelHubScreen.kt` line 40-42 (before fix)

```kotlin
var activeDownload by remember {
    mutableStateOf(modelManager.getActiveDownloadFlow().value)
}
```

**Problem:**
This code captured the **initial value** of the `StateFlow` at composition time and stored it in a separate `MutableState`. This created a **decoupled state** that:
1. Only received the flow's value once at initialization
2. Was not automatically updated when the flow emitted new values
3. Required manual synchronization via a separate `LaunchedEffect`

### Secondary Issue: Redundant LaunchedEffect
**Location:** `ModelHubScreen.kt` line 66-81 (before fix)

```kotlin
LaunchedEffect(modelManager) {
    modelManager.getActiveDownloadFlow().collect { active ->
        activeDownload = active
        when (active?.state) {
            is DownloadState.Completed,
            is DownloadState.Failed -> {
                try {
                    models = modelManager.getAvailableModels()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to refresh models after download", e)
                }
            }
            else -> Unit
        }
    }
}
```

**Problem:**
This LaunchedEffect was attempting to manually synchronize the decoupled state by:
1. Collecting the flow
2. Manually updating the `activeDownload` mutable state
3. This approach is error-prone and can have timing issues

### Why This Happened
The architecture has two separate state management layers:
1. **Domain layer:** `ModelManagerWrapper` maintains `_activeDownload` StateFlow
2. **UI layer:** `ModelHubScreen` was creating its own separate mutable state

The UI layer was not properly observing the domain layer's StateFlow, leading to a state synchronization gap. When the download started:
- `ModelManagerWrapper.download()` updated `_activeDownload` flow
- `ModelHubScreen`'s `activeDownload` state remained at its initial value
- UI did not recompose because the state it was observing didn't change

## The Fix

### Change 1: Use collectAsState() Directly
**File:** `ModelHubScreen.kt` line 40

**Before:**
```kotlin
var activeDownload by remember {
    mutableStateOf(modelManager.getActiveDownloadFlow().value)
}
```

**After:**
```kotlin
val activeDownload by modelManager.getActiveDownloadFlow().collectAsState()
```

**Rationale:**
- `collectAsState()` automatically subscribes to the StateFlow
- Ensures the UI state is always synchronized with the flow
- Eliminates the need for manual state synchronization
- Triggers recomposition whenever the flow emits a new value

### Change 2: Simplified LaunchedEffect
**File:** `ModelHubScreen.kt` line 64-76

**Before:**
```kotlin
LaunchedEffect(modelManager) {
    modelManager.getActiveDownloadFlow().collect { active ->
        activeDownload = active
        when (active?.state) {
            is DownloadState.Completed,
            is DownloadState.Failed -> {
                try {
                    models = modelManager.getAvailableModels()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to refresh models after download", e)
                }
            }
            else -> Unit
        }
    }
}
```

**After:**
```kotlin
LaunchedEffect(activeDownload?.state) {
    when (activeDownload?.state) {
        is DownloadState.Completed,
        is DownloadState.Failed -> {
            try {
                models = modelManager.getAvailableModels()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh models after download", e)
            }
        }
        else -> Unit
    }
}
```

**Rationale:**
- Removed manual flow collection (now handled by collectAsState)
- Changed key from `modelManager` (never changes) to `activeDownload?.state` (changes on download state)
- Only refreshes models list when download completes or fails
- Cleaner separation of concerns

## State Flow After Fix

### Download Start Flow
1. User clicks "Add to App"
2. `ModelManagerWrapper.download()` is called
3. `_activeDownload` flow emits new value with `DownloadState.Downloading` state
4. `collectAsState()` in ModelHubScreen receives the emission
5. `activeDownload` state updates automatically
6. `displayModels` remember block recomputes with new `activeDownload`
7. `withActiveDownload()` merges the download state into the model
8. UI recomposes and shows `DownloadProgressCard`

### Download Complete Flow
1. Download finishes
2. `_activeDownload` flow emits `DownloadState.Completed`
3. `collectAsState()` updates `activeDownload`
4. LaunchedEffect triggers on state change (Completed/Failed)
5. Calls `modelManager.getAvailableModels()` to refresh models
6. Model now has `ModelDownloadState.Installed` state
7. UI shows `AppCard` with "Remove" button

## Verification Steps
1. Click "Add to App" on a model
2. Verify card immediately changes to DownloadProgressCard
3. Verify progress updates in real-time
4. After download completes, verify card changes to show "Remove" button
5. Verify no excessive registry fetching (should only fetch when needed)

## Related Files
- `axiom-android-sdk/src/main/java/com/axiom/android/sdk/ui/screens/ModelHubScreen.kt`
- `axiom-android-sdk/src/main/java/com/axiom/android/sdk/models/ModelManagerWrapper.kt`
- `axiom-android-sdk/src/main/java/com/axiom/android/sdk/domain/DownloadState.kt`

## Lessons Learned
1. **Always use `collectAsState()` for StateFlow observation in Compose** - never manually create mutable state from flow values
2. **Avoid dual state management** - don't create separate UI state when domain layer already has observable state
3. **LaunchedEffect keys matter** - use keys that actually change to trigger recomposition
4. **State synchronization should be automatic** - manual synchronization is error-prone
