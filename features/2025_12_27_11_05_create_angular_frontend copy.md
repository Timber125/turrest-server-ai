Instruction: 

this backend serves as a game lobby backbone which can start multiple games; currently only one game is supported and unfinished, called TURREST-mode1.

The engine for this game is unfinished, and will currently only send "GameTileChangedCommands" which makes tiles change type. This was a proof of concept.

I would like you to create an angular frontend for this game, put it next to "src" in a folder called "frontend".

Communication happens trough named commands, containing topic/subject/hashmap-data representing actions.

All game engine related calculations happen serversided - frontend only initiates actions for server & renders display changes received in commands from backend. 

--------------

## Implementation Plan

### Phase 1: Project Setup

1. **Initialize Angular project** in `/frontend`
   - Use Angular CLI with standalone components
   - Configure for development proxy to backend (port 8081)
   - Add Angular Material for UI components

2. **Project structure:**
   ```
   frontend/
   ├── src/
   │   ├── app/
   │   │   ├── core/                    # Core services & guards
   │   │   │   ├── services/
   │   │   │   │   ├── auth.service.ts
   │   │   │   │   ├── socket.service.ts
   │   │   │   │   └── game.service.ts
   │   │   │   ├── guards/
   │   │   │   └── interceptors/
   │   │   ├── features/
   │   │   │   ├── auth/                # Login/Register pages
   │   │   │   ├── lobby/               # Lobby list & management
   │   │   │   └── game/                # Game view & tile rendering
   │   │   ├── shared/                  # Shared components & models
   │   │   │   ├── models/
   │   │   │   └── components/
   │   │   └── app.component.ts
   │   └── environments/
   ```

---

### Phase 2: Core Services

3. **Authentication Service** (`auth.service.ts`)
   - REST calls to `POST /login` and `POST /register`
   - Store token, userId, username in memory/localStorage
   - Token valid for 4 hours
   - Methods: `login()`, `register()`, `logout()`, `isAuthenticated()`

4. **Socket Service** (`socket.service.ts`)
   - Backend uses raw TCP sockets:
     - Port 1234 for lobby connections
     - Random port allocated when a game starts
   - **Browser compatibility:** Browsers CANNOT connect to raw TCP sockets. The browser `WebSocket` API requires WebSocket protocol (HTTP upgrade handshake), which raw Java `ServerSocket` does not support.
   - **Required backend change:** Add Spring WebSocket support (`spring-boot-starter-websocket`) to enable browser connectivity. This will wrap the existing command protocol in WebSocket frames.
   - Implement command protocol:
     - `sendCommand(subject, topic, data)` - Send ClientToServerCommand
     - `onCommand(subject, topic)` - Observable for receiving ServerToClientCommands
   - Handle ConnectionCommand on connect
   - Reconnection logic with exponential backoff

5. **Command Models** (`shared/models/`)
   ```typescript
   interface SocketCommand {
     subject: string;
     topic: string;
     data: Record<string, any>;
   }

   // Subjects: LOBBY, GLOBAL_CHAT, SOCKET_CONNECT, DISPLAY_CHAT
   // Topics: LOGIN, GET_ALL, CREATE, JOIN, START_GAME, GLOBAL, etc.
   ```

---

### Phase 3: Authentication UI

6. **Login Component** (`features/auth/login/`)
   - Form: username, password
   - Call `authService.login()`
   - On success: Connect socket, send ConnectionCommand, navigate to lobby

7. **Register Component** (`features/auth/register/`)
   - Form: username, password, confirm password
   - Call `authService.register()`
   - Auto-login after registration

8. **Auth Guard** - Protect lobby/game routes for authenticated users only

---

### Phase 4: Lobby System

9. **Lobby List Component** (`features/lobby/lobby-list/`)
   - Send `RefreshLobbiesCommand` on init
   - Display lobbies in a table/card grid:
     - Lobby ID, Game type, Size, Hidden status
     - "Join" button per lobby
   - "Create Lobby" button → opens dialog
   - Auto-refresh on interval or via server push

10. **Create Lobby Dialog** (`features/lobby/create-lobby/`)
    - Form: size (2-8), hidden (checkbox), password (optional), game type (TURREST-mode1)
    - Send `CreateLobbyCommand`
    - On `LobbyCreatedResponse` → navigate to lobby room

11. **Lobby Room Component** (`features/lobby/lobby-room/`)
    - Show connected players (poll or implement player list command)
    - Chat panel (GlobalChat for now)
    - "Start Game" button (visible only to host = players[0])
    - "Leave Lobby" button
    - Listen for `GameStartedResponse` → navigate to game view

---

### Phase 5: Chat System

12. **Chat Component** (`shared/components/chat/`)
    - Display chat messages from `DisplayChatCommand`
    - Input field to send `GlobalChatCommand`
    - Timestamp formatting
    - Scrollable message history

---

### Phase 6: Game View (TURREST-mode1)

13. **Game Component** (`features/game/game.component.ts`)
    - Canvas or HTML grid for tile-based map
    - TerrainType enum mapping to colors/sprites:
      - GRASS(1) → green
      - DIRT(2) → brown
      - FOREST(3) → dark green
      - WATER_SHALLOW(4) → light blue
      - WATER_DEEP(5) → dark blue
      - ROCKY(6) → gray
      - CASTLE(7) → castle sprite
      - SPAWNER(8) → special tile
    - Listen for `GameTileChangedCommand` (future)
    - Render Building overlays on tiles

14. **Tile Renderer** - Canvas-based or CSS Grid rendering system
    - Efficient re-rendering on tile updates
    - Click handlers for future player actions

---

### Phase 7: State Management

15. **App State** (using signals or NgRx if needed):
    - `currentUser: { id, username, token }`
    - `currentLobby: { id, size, players, game }`
    - `gameState: { map: Tile[][], players: Player[] }`
    - `chatMessages: Message[]`

---

### Backend Modifications Required

16. **Add WebSocket endpoint** - **COMPLETED**
    - Added `spring-boot-starter-websocket` dependency
    - Created `ClientSession` interface to abstract connection types
    - Created `WebSocketConfig` - configures `/ws/lobby` endpoint
    - Created `WebSocketClientSession` - implements ClientSession for WebSocket
    - Created `WebSocketConnectionHandler` - bridges WebSocket to command protocol
    - Updated all code to use `ClientSession` instead of `SocketHandler`
    - Both TCP (port 1234) and WebSocket (`/ws/lobby`) now supported

---

### File Dependencies

| Frontend File | Backend Interaction |
|---------------|---------------------|
| `auth.service.ts` | REST: `/login`, `/register` |
| `socket.service.ts` | WebSocket (to be added) or TCP bridge |
| `lobby-list.component.ts` | Commands: GET_ALL, JOIN |
| `create-lobby.component.ts` | Commands: CREATE |
| `lobby-room.component.ts` | Commands: START_GAME, GLOBAL_CHAT |
| `game.component.ts` | Commands: TILE_CHANGED (future) |

---

### Implementation Order

1. ✅ **DONE** - Backend: Add WebSocket support (required for browser)
2. Angular project setup + routing
3. Auth service + login/register UI
4. Socket service + ConnectionCommand
5. Lobby list + create/join
6. Lobby room + chat + start game
7. Game view + tile rendering
8. Polish: error handling, loading states, reconnection

---

### Notes

- Backend token expires in 4 hours
- Lobby ID equals host player UUID
- All game logic is server-side - frontend only renders
- GameTileChangedCommand is not yet implemented in backend
- Currently only TURREST-mode1 game type supported