# RCA: IME GC Suppression Crash

## Summary
**Issue**: Application logged `NullPointerException` in `callGcSupression` during keyboard IME show/hide operations.

**Status**: Workaround applied

**Date**: April 20, 2026

**Affected Component**: Android framework (IME/keyboard handling)

---

## Problem Description

### Error Message
```
java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.Object java.lang.reflect.Method.invoke(java.lang.Object, java.lang.Object[])' on a null object reference
```

### Log Pattern
```
2026-04-20 01:47:30.486  5130-5130  callGcSupression        com.axiom.sample                     D  java.lang.NullPointerException: ...
2026-04-20 01:47:30.664  5130-5130  callGcSupression        com.axiom.sample                     D  java.lang.NullPointerException: ...
2026-04-20 01:47:30.665  5130-5130  VRI[MainActivity]       com.axiom.sample                     W  handleResized abandoned!
```

### Impact
- Error logged during keyboard show/hide operations
- No application crash (error is caught by framework)
- May cause UI jank or delayed keyboard response
- Does not affect core functionality

---

## Root Cause Analysis

### Investigation Steps

1. **Identified Error Location**
   - Error tagged with `callGcSupression` (Android framework code)
   - Occurs during IME show/hide operations
   - Not present in application codebase

2. **Analyzed the Error**
   - Reflection `Method.invoke()` called on null Method object
   - Related to garbage collection suppression in Android framework
   - Triggered by window resize during keyboard animations

3. **Traced Trigger**
   - Manifest has `android:windowSoftInputMode="adjustResize"`
   - Window resizes when keyboard appears/disappears
   - Framework attempts GC suppression during resize
   - Reflection call fails on certain Android versions/devices

### Root Cause
**Android framework bug in GC suppression logic during IME window resize operations.** The framework uses reflection to call a method that may not exist on certain Android versions or device configurations, causing a NullPointerException that is caught and logged but does not crash the app.

---

## Solution Implemented

### Workaround Applied

#### File: `sample/src/main/AndroidManifest.xml`

Changed windowSoftInputMode from `adjustResize` to `adjustPan`:

```xml
<!-- Before -->
android:windowSoftInputMode="adjustResize">

<!-- After -->
android:windowSoftInputMode="adjustPan">
```

### Rationale
- `adjustPan` shifts the content instead of resizing the window
- Avoids triggering the framework's GC suppression logic
- Maintains keyboard functionality without the error
- Minor UI difference (content pans vs resizes)

---

## Verification

### Build Verification
```bash
./gradlew :sample:assembleDebug
```

### Testing Steps
1. Install updated APK on device/emulator
2. Open chat interface
3. Show/hide keyboard multiple times
4. Verify no `callGcSupression` errors in logcat
5. Verify keyboard interaction works correctly

### Expected Behavior
- No `callGcSupression` errors in logcat
- Keyboard shows/hides smoothly
- Content pans correctly when keyboard appears
- No UI jank or delays

---

## Alternative Solutions

### Option 1: Suppress the Error Log
Add a logcat filter to ignore the error:
```bash
adb logcat *:S callGcSupression:V
```
**Pros**: No code changes
**Cons**: Error still occurs, just hidden

### Option 2: Keep adjustResize with Error Handling
Keep `adjustResize` and accept the framework error (it's caught and doesn't crash).
**Pros**: Better UI experience (content resizes)
**Cons**: Error logs still appear

### Option 3: Custom Keyboard Handling
Implement custom keyboard handling with WindowInsets API.
**Pros**: Full control over keyboard behavior
**Cons**: Significant development effort

---

## Lessons Learned

### Technical Insights

1. **Android Framework Bugs**
   - Framework code can have bugs that affect apps
   - Some bugs are device/version specific
   - Errors may be caught and logged without crashing

2. **WindowSoftInputMode Trade-offs**
   - `adjustResize`: Better UX, may trigger framework bugs
   - `adjustPan`: Safer, different UX behavior
   - Choice depends on priority (UX vs stability)

3. **IME Operations Complexity**
   - Keyboard show/hide involves complex window operations
   - Framework uses reflection for compatibility
   - Reflection can fail on certain configurations

### Process Improvements

1. **Logcat Analysis**
   - Pay attention to framework-level errors
   - Distinguish between app and framework code
   - Some errors are harmless (caught by framework)

2. **Device Testing**
   - Test on multiple Android versions
   - Test on different device manufacturers
   - Framework behavior can vary

3. **Documentation**
   - Document framework bugs and workarounds
   - Track which devices/versions are affected
   - Update workarounds as Android versions evolve

---

## Related Files

### Modified Files
- `sample/src/main/AndroidManifest.xml`
  - Changed `windowSoftInputMode` from `adjustResize` to `adjustPan`

### Related Documentation
- `docs/chat-keyboard-layout-rca.md` - Keyboard layout issues
- `LOG_READER.md` - Logcat filtering instructions

---

## References

### Android Documentation
- [WindowSoftInputMode](https://developer.android.com/guide/topics/manifest/activity-element#wsoft)
- [WindowInsets API](https://developer.android.com/reference/android/view/WindowInsets)
- [IME Handling](https://developer.android.com/training/keyboard-input/visibility)

---

## Follow-up Actions

1. **Monitoring**
   - Monitor for `adjustPan` UX issues
   - Track user feedback on keyboard behavior
   - Consider reverting to `adjustResize` if UX is problematic

2. **Future Improvements**
   - Test on Android 15+ when available (framework may be fixed)
   - Consider implementing custom keyboard handling if needed
   - Evaluate Jetpack WindowInsets API for better control

3. **Testing**
   - Test keyboard behavior on multiple devices
   - Test with different keyboard apps
   - Verify no regression in chat functionality

---

**Author**: Cascade AI Assistant
**Last Updated**: April 20, 2026
