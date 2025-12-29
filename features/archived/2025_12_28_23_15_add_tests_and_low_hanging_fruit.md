Instructions:

I want you to add distinct names to the lobby - "<player name>'s lobby" by default, and editable by the host.
Then, i'd like you to write tests for the lobby system in backend. If any scenario's point towards bugs in code, fix them.

---

## Implementation Plan

### Part 1: Add Distinct Lobby Names

#### 1.1 Backend Model Changes

**File: `src/main/java/be/lefief/lobby/Lobby.java`**
- Add `name` field (String) to store lobby name
- Initialize default name as `"<hostName>'s lobby"` in constructor
- Add getter/setter for name
- Add validation (non-empty, max length ~50 chars)

**File: `src/main/java/be/lefief/lobby/LobbyPlayer.java`**
- No changes needed (already has `name` field)

#### 1.2 Backend Commands

**File: `src/main/java/be/lefief/sockets/commands/client/emission/CreateLobbyCommand.java`**
- Add optional `name` field to allow custom name on creation

**New File: `src/main/java/be/lefief/sockets/commands/client/emission/RenameLobbyCommand.java`**
- Create command with `newName` field for host to rename lobby

**File: `src/main/java/be/lefief/sockets/commands/client/reception/LobbyStateResponse.java`**
- Ensure `name` field is included in lobby state responses

**File: `src/main/java/be/lefief/sockets/commands/client/reception/RefreshLobbiesResponse.java`**
- Ensure lobby name is included in lobby list items

#### 1.3 Backend Handlers

**New File: `src/main/java/be/lefief/sockets/handlers/routing/RenameLobbyHandler.java`**
- Route RenameLobbyCommand to secured command format

**New File: `src/main/java/be/lefief/sockets/handlers/lobby/RenameLobbyListener.java`**
- Listen for rename commands, delegate to LobbyHandler

**File: `src/main/java/be/lefief/sockets/handlers/LobbyHandler.java`**
- Add `handleRenameLobby()` method
- Validate: only host can rename, lobby not started
- Broadcast updated LobbyStateResponse to all players

**File: `src/main/java/be/lefief/service/lobby/LobbyService.java`**
- Update `createLobby()` to accept optional name parameter
- Add `renameLobby()` method with host validation

#### 1.4 Frontend Changes

**File: `frontend/src/app/shared/models/lobby.model.ts`**
- Add `name` field to Lobby interface

**File: `frontend/src/app/core/services/lobby.service.ts`**
- Add `renameLobby(newName: string)` method
- Handle rename response/error

**File: `frontend/src/app/features/lobby/lobby-list/lobby-list.component.ts`**
- Display lobby name instead of/alongside lobby ID in list
- Add optional name field to create lobby dialog

**File: `frontend/src/app/features/lobby/lobby-room/lobby-room.component.ts`**
- Display lobby name at top of room
- Add edit button (pencil icon) next to name for host only
- Implement inline editing or modal for renaming

---

### Part 2: Lobby System Unit Tests

#### 2.1 Test Setup

**New File: `src/test/java/be/lefief/lobby/LobbyTest.java`**
Unit tests for `Lobby` class:
- `testCreateLobby_defaultValues()` - verify initial state
- `testAddClient_success()` - add player to open lobby
- `testAddClient_lobbyFull()` - reject when at max capacity
- `testAddClient_lobbyStarted()` - reject when game started
- `testRemoveClient_normalPlayer()` - remove non-host player
- `testRemoveClient_host()` - host leaves, lobby destroyed/closed
- `testChangePlayerColor_validColor()` - change to available color
- `testChangePlayerColor_colorTaken()` - reject duplicate color
- `testChangePlayerColor_playerReady()` - reject when player is ready
- `testTogglePlayerReady()` - toggle ready state
- `testAllPlayersReady_allReady()` - returns true when all ready
- `testAllPlayersReady_someNotReady()` - returns false when not all ready
- `testStart_allReady()` - successfully start game
- `testStart_notAllReady()` - reject start when not all ready
- `testLobbyName_defaultName()` - verify default "<player>'s lobby"
- `testLobbyName_customName()` - verify custom name setting
- `testLobbyName_rename()` - verify rename functionality

**New File: `src/test/java/be/lefief/lobby/LobbyPlayerTest.java`**
Unit tests for `LobbyPlayer` class:
- `testCreateLobbyPlayer()` - verify construction
- `testSetReady()` - toggle ready state
- `testSetColorIndex()` - change color

#### 2.2 Service Layer Tests

**New File: `src/test/java/be/lefief/service/lobby/LobbyServiceTest.java`**
Unit tests for `LobbyService`:
- `testCreateLobby_success()` - create lobby, verify stored
- `testCreateLobby_withCustomName()` - create with custom name
- `testJoinLobby_success()` - join existing lobby
- `testJoinLobby_lobbyNotFound()` - error for invalid lobby ID
- `testJoinLobby_lobbyFull()` - error when lobby at capacity
- `testJoinLobby_lobbyStarted()` - error when game in progress
- `testJoinLobby_wrongPassword()` - error for protected lobby
- `testGetLobbies_filtersHidden()` - hidden lobbies not in list
- `testGetLobbies_filtersStarted()` - started games not in list
- `testFindLobbyByPlayer()` - locate player's lobby
- `testRenameLobby_asHost()` - host can rename
- `testRenameLobby_notHost()` - non-host cannot rename
- `testHandleLogout_removesFromLobby()` - player removed on disconnect

#### 2.3 Handler Tests

**New File: `src/test/java/be/lefief/sockets/handlers/LobbyHandlerTest.java`**
Integration tests for `LobbyHandler`:
- `testHandleCreateLobby_sendsConfirmation()` - verify responses sent
- `testHandleJoinLobby_broadcastsState()` - all players get update
- `testHandleChangeColor_broadcastsState()` - color change broadcast
- `testHandleToggleReady_broadcastsState()` - ready toggle broadcast
- `testHandleStartLobbyGame_allReady()` - game starts successfully
- `testHandleStartLobbyGame_notAllReady()` - error when not all ready
- `testHandleStartLobbyGame_notHost()` - only host can start
- `testHandleRenameLobby_success()` - rename broadcasts new state

---

### Potential Bug Scenarios to Investigate

1. **Race condition on join**: Two players join simultaneously when only 1 slot available
2. **Host disconnect handling**: Does lobby properly close/transfer when host disconnects?
3. **Color collision**: Can two players end up with same color in edge cases?
4. **Ready state after color change**: Is ready state properly reset when color changes?
5. **Lobby cleanup**: Are empty lobbies properly removed from memory?
6. **Password validation**: Is password checked correctly for protected lobbies?
7. **Max players boundary**: Edge cases at exactly max capacity
8. **Started lobby rejoin**: Can players rejoin a started game after disconnect?

---

### Implementation Order

1. **Backend model changes** (Lobby.java name field)
2. **Write Lobby unit tests** (will reveal any existing bugs)
3. **Fix any bugs found**
4. **Add rename command & handlers**
5. **Write LobbyService tests**
6. **Write LobbyHandler tests**
7. **Frontend model updates**
8. **Frontend lobby list updates** (show names)
9. **Frontend lobby room updates** (rename UI)
10. **End-to-end testing** 
