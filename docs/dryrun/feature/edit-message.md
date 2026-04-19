# Edit Message - Dry Run Guide

## Feature Overview
Edit message functionality allows users to edit their previous messages, which deletes all subsequent messages and regenerates the assistant response from that point.

## Implementation Details
- **Dialog**: `EditMessageDialog.kt`
- **UI**: Edit button on user messages in `ChatMessageItem`
- **Logic**: Delete messages after edit, regenerate from edited point
- **Database**: Uses `repository.deleteMessagesAfter()` for cleanup

## Dry Run Steps

### 1. Start a Conversation
- Open ChatScreen with a session
- Send a message: "What is 2 + 2?"
- Wait for assistant response
- Send a follow-up: "What about 3 + 3?"
- Wait for assistant response

### 2. Test Edit Dialog
- Click the edit button on the first user message
- Verify EditMessageDialog opens with original content
- Check that the dialog has:
  - Text input field with original message
  - Cancel button
  - Save & Regenerate button (enabled when content is not blank)

### 3. Test Message Edit
- Change the message to: "What is 5 + 5?"
- Click "Save & Regenerate"
- Verify the dialog closes
- Verify the message is updated
- Verify all messages after the edited message are deleted
- Verify a new assistant response is generated

### 4. Verify Database Cleanup
- Check logs for: `repository.deleteMessagesAfter(sessionId, timestamp)`
- Verify that deleted messages are removed from database
- Verify the edited message is updated in database
- Verify the new assistant response is saved

### 5. Test Edit During Generation
- Start a new message generation
- Try to click edit button (should be disabled)
- Verify edit button is disabled while generating

### 6. Test Empty Edit
- Click edit on a message
- Clear the text field
- Verify "Save & Regenerate" button is disabled
- Click Cancel to close dialog
- Verify no changes were made

### 7. Test Edit on Assistant Message
- Try to find edit button on assistant message
- Verify edit button only appears on user messages
- Verify regenerate button appears on assistant messages

## Success Criteria
- ✓ Edit dialog opens correctly
- ✓ Original message content is pre-filled
- ✓ Edit button disabled during generation
- ✓ Messages after edit are deleted
- ✓ Assistant response regenerates from edited point
- ✓ Database is properly updated
- ✓ Cancel button works without changes
- ✓ Save button disabled for empty content

## Known Issues
- None identified

## Rollback Plan
If issues occur:
1. Remove edit button from ChatMessageItem
2. Remove EditMessageDialog import from ChatScreen
3. Remove edit dialog state and logic from ChatScreen
4. Restore original message handling without edit functionality
