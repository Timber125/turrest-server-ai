Instruction: 

We've done a great job prototyping the game; 
However, connection instabilities are still an issue. 

- A player closing their browser window should make the client disconnect. 
- A player reopening their browser window should make the client reconnect to a game if they are in one. 
- A player reconnecting to a game should receive a new copy of the current game map & game state from the server 
- A player disconnecting from the game by choice (button) should make the client disconnect + remove the player from the game.
- If a player is last connected client to a game, that player wins the game. Game should be cleaned up & player should be redirected to lobby. 
- If a player's token is no longer valid & the player is in an active game, the token should refresh & the player should be able to continue playing seemlessly. 
- If a player's token is no longer valid & the player is in the lobby, the player should be redirected to login to login again. 
- A player losing the game should be removed from the game player list, but should be allowed to continue spectating the other players (if the game is not over yet)

--------------

# Connection Robustness Implementation Plan

This plan addresses connection stability, reconnection logic, and game state synchronization.

## Backend Changes

### 1. Game Service Updates (`be.lefief.game.GameService`)
-   **Track Player Activity**: Introduce a `Map<UUID, UUID> playerActiveGame` to track which game (GameID) a player (UserID) is currently participating in.
-   **Methods**:
    -   `registerPlayer(UUID userId, UUID gameId)`: Call when game starts.
    -   `unregisterPlayer(UUID userId)`: Call when game ends or player leaves explicitly.
    -   `isPlayerInGame(UUID userId)`: Check if player has an active game.
    -   `reconnectPlayer(UUID userId, ClientSession newSession)`: Logic to handle reconnection.

### 2. Connection Handling (`be.lefief.sockets.handlers.ConnectionHandler`)
-   **Reconnection Check**: In the `accept` method (after successful auth), check `gameService.isPlayerInGame(userId)`.
    -   **If yes**: Delegate to `gameService.reconnectPlayer`. This should bypass Lobby login and directly re-attach the user to the game.
    -   **If no**: Proceed with normal `lobbyService.handleLogin`.

### 3. Game State Synchronization (`be.lefief.game.Game`, `TurrestGameMode01`)
-   **Method `resyncPlayer(Player player)`**:
    -   Send current Map state (`TILE_CHANGED` events or a bulk `MAP_DATA` event).
    -   Send current Entities/Units state.
    -   Send current Resources/Score.

### 4. Disconnect & Win Condition (`WebSocketConnectionHandler`, `Game`)
-   **Disconnect Detection**: In `WebSocketConnectionHandler.afterConnectionClosed`, notify `GameService`.
-   **Game State Tracking**: Introduce a `boolean gameIsRunning` flag (default true) in `Game` class.
-   **Game Logic**:
    -   When a player disconnects, mark them as `DISCONNECTED`.
    -   **Win Check on Disconnect**: Check if `active_connected_players == 1` **AND** `gameIsRunning`.
        -   If true:
            -   Set `gameIsRunning = false`.
            -   Trigger `EndGameCommand` declaring the remaining player as winner.
            -   Cleanup game in `GameService`.
    -   **Normal Win Check**: Ensure standard win conditions also set `gameIsRunning = false` to prevent race conditions or subsequent disconnects triggering a second win.

### 5. Explicit Leave
-   **New Command**: Handle `LEAVE_GAME` command from client.
-   **Action**: Unregister player, remove from game, trigger "Player Left" notification to others.

## Frontend Changes

### 1. Token Refresh (`AuthService`, `SocketService`)
-   **Interceptor/Logic**: If WebSocket connection fails with `401/403` or custom "Invalid Token" message:
    -   Attempt `refreshToken()`.
    -   If successful, retry WebSocket connection with new token.
    -   If failure, redirect to `/login`.

### 2. Socket Service (`SocketService`)
-   **Auto-Reconnect**: Ensure the socket attempts to reconnect if the connection drops unexpectedly (using `RxJS` backoff/retry strategies).
-   **State Handling**: Handle "Bulk Map Data" if we decide to implement that for optimization, or ensure the existing `TILE_CHANGED` handler works for a flood of events.

## Task Breakdown

- [ ] **Backend Core** <!-- id: 100 -->
    - [ ] Update `GameService` to track player->game mapping. <!-- id: 101 -->
    - [ ] Update `ConnectionHandler` to handle reconnection. <!-- id: 102 -->
    - [ ] Implement `resyncPlayer` in `Game`/`TurrestGameMode01`. <!-- id: 103 -->
- [ ] **Backend Disconnects** <!-- id: 104 -->
    - [ ] Hook into `socket.close` events. <!-- id: 105 -->
    - [ ] Implement "Last Player Standing" logic. <!-- id: 106 -->
- [ ] **Frontend Robustness** <!-- id: 107 -->
    - [ ] Implement Token Refresh on socket error. <!-- id: 108 -->
    - [ ] Ensure `SocketService` reconnects automatically. <!-- id: 109 -->
- [ ] **Manual Verification** <!-- id: 110 -->
    - [ ] Test Browser Close -> Wait -> Reopen (Expect: Rejoin Game). <!-- id: 111 -->
    - [ ] Test Button "Leave" (Expect: Lobby). <!-- id: 112 -->
    - [ ] Test Win Condition (Connect 2 players, close 1) (Expect: Winner). <!-- id: 113 -->
