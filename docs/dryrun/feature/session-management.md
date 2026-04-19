# Session Management - Dry Run Guide

## Feature Overview
Session management provides persistent storage for chat conversations using Room database, allowing users to create, switch, and delete chat sessions.

## Implementation Details
- **Entities**: `ChatSession`, `ChatMessageEntity`
- **DAOs**: `ChatSessionDao`, `ChatMessageDao`
- **Repository**: `ChatSessionRepository`
- **Database**: `AxiomDatabase` with Room
- **UI**: `SessionListScreen`

## Dry Run Steps

### 1. Database Setup Verification
```bash
# Check Room dependencies are added
grep "room-runtime" axiom-android-sdk/build.gradle.kts
grep "room-ktx" axiom-android-sdk/build.gradle.kts
grep "room-compiler" axiom-android-sdk/build.gradle.kts
```

### 2. Build Application
```bash
./gradlew clean
./gradlew assembleDebug
```

### 3. Test Session Creation
- Open the app
- Navigate to SessionListScreen
- Click the "+" button to create a new session
- Verify a new session is created with title "New Chat"
- Check that the session appears in the list

### 4. Test Chat with Session
- Select a session from the list
- Send a message in the chat
- Verify the message is saved to database
- Kill and restart the app
- Navigate back to the session
- Verify messages persist

### 5. Test Session List
- Create multiple sessions
- Verify all sessions appear in list
- Check that sessions are sorted by updatedAt (newest first)
- Verify session titles, model IDs, and dates display correctly

### 6. Test Session Deletion
- Long-press or click delete on a session
- Confirm deletion in dialog
- Verify session and all messages are deleted
- Check database is cleaned up

### 7. Test Session Switching
- Create Session A with messages
- Create Session B with different messages
- Switch between sessions
- Verify each session maintains its own message history

### 8. Verify Database Operations
Check logs for database operations:
```
ChatSessionRepository: Creating session
ChatSessionRepository: Adding message
ChatSessionRepository: Updating session timestamp
```

## Success Criteria
- ✓ Sessions can be created
- ✓ Messages are saved to database
- ✓ Messages persist across app restarts
- ✓ Sessions can be deleted
- ✓ Session switching works correctly
- ✓ Session list displays correctly
- ✓ No database errors in logs

## Known Issues
- None identified

## Rollback Plan
If issues occur:
1. Remove Room dependencies from build.gradle.kts
2. Delete database entities, DAOs, and repository files
3. Remove SessionListScreen
4. Revert ChatScreen to in-memory message storage
