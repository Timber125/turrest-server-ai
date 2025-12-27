# Implementation Plan - Additional Robustness Fixes

This plan addresses several UX and stability improvements for the game's connection and lifecycle management.

## 1. Authentication Timeout Fix
**Goal:** Prevent the "did not receive login information in time" message from appearing prematurely or after successful login.

*   **Backend (`WebSocketClientSession.java`, `SocketHandler.java`)**:
    *   Store the `TimerTask` as a member variable.
    *   In `authenticate(...)`, call `timerTask.cancel()` to stop the timeout message once the user is identified.
    *   Increase the default timeout from 3 seconds to 10 seconds to allow for network latency.

## 2. Per-Tab Session Support
**Goal:** Allow a single user to open multiple tabs and have them operate independently (e.g., joining as two different players or simply not conflicting).

*   **Frontend (`SocketService.ts`)**:
    *   Generate a unique `tabId` (UUID) upon service initialization and store it in `sessionStorage`.
    *   Include this `tabId` in the `SOCKET_CONNECT` command data.
*   **Backend (`ClientSession.java`, `WebSocketClientSession.java`, `SocketHandler.java`)**:
    *   Add `getTabId()` and `setTabId(String id)` to the `ClientSession` interface.
*   **Backend (`LobbyService.java`)**:
    *   Update `identifiedClients` to be `Map<String, ClientSession>` where the key is `userId + ":" + tabId`.
    *   Update all lookups to use this composite key.
*   **Backend (`GameService.java`)**:
    *   Update `playerActiveGame` to be keyed by `userId + ":" + tabId`.
    *   Ensure reconnection logic uses the composite key to find the correct session to replace.

## 3. Lobby Lifecycle Management
**Goal:** Hide lobbies when they start and destroy them when the game ends.

*   **Backend (`Lobby.java`)**:
    *   Add an `active` boolean field.
*   **Backend (`LobbyService.java`)**:
    *   Modify `getLobbies()` to only return lobbies where `active == true`.
    *   Add `archiveLobby(UUID hostId)` to set `active = false`.
    *   Add `removeLobby(UUID hostId)` to delete it from `lobbyHosts`.
*   **Backend (`LobbyHandler.java`)**:
    *   In `handleStartLobbyGame`, call `lobbyService.archiveLobby(hostId)`.
*   **Backend (`GameService.java`)**:
    *   Update `cleanupGame(UUID gameId)` to also trigger `lobbyService.removeLobby(hostId)`. (Note: Will need to track the `hostId` in the `Game` instance).

## 4. Game Start Countdown
**Goal:** Give players 5 seconds to load the game view before the map data flood starts and the game loop begins.

*   **Backend (`CountdownResponse.java`)**:
    *   Create a new server-to-client command to notify the client of the countdown.
*   **Backend (`TurrestGameMode01.java`)**:
    *   Modify `start()`:
        1.  Broadcast `CountdownResponse(5)`.
        2.  Initialize the `gameMap` from the level file.
        3.  Schedule a one-shot task to run after 5 seconds:
            -   Call `sendInitialMapToPlayers()`.
            -   Call `startGameLoop()`.

## 5. Verify Frontend Logic
*   **Frontend (`GameComponent`)**:
    *   Ensure the "Waiting for map data" overlay handles the countdown state if we want to display it visually.

---

## Tasks

- [x] **Auth Fix**
    - [x] Cancel timer on auth in `WebSocketClientSession` & `SocketHandler`.
- [x] **Tab Sessions**
    - [x] Update frontend `SocketService` to generate and send `tabId`.
    - [x] Update backend `ClientSession` and `LobbyService` to use composite keys.
- [x] **Lobby Lifecycle**
    - [x] Add `active` flag and filtering in `LobbyService`.
    - [x] Archive on start, remove on stop.
- [x] **Countdown**
    - [x] Implement `CountdownResponse` command.
    - [x] Add 5s delay logic to `TurrestGameMode01.start()`.
