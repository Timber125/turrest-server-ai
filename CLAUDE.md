# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Turrest is a real-time multiplayer tower defense game with a Spring Boot 2.7 backend (Java 17) and Angular 21 frontend. Players place towers, send creeps to opponents, and manage resources (wood, stone, gold) to be the last survivor.

## Build & Run Commands

### Backend (Maven)
```bash
mvn clean install              # Build
mvn spring-boot:run            # Run server on port 8081
mvn test                       # Run all tests
mvn test -Dtest=LobbyTest      # Run specific test class
mvn flyway:migrate             # Run database migrations
```

### Frontend (Angular)
```bash
cd frontend
npm install                    # Install dependencies
npm start                      # Dev server on localhost:4200
npm run build                  # Production build
npm test                       # Run tests (Vitest)
```

### Full Stack Development
Run backend (`mvn spring-boot:run`) and frontend (`npm start`) in separate terminals. Browser at http://localhost:4200, WebSocket at ws://localhost:8081/ws/lobby.

## Architecture

### Game Modes
- **Turrest01**: Basic mode - 2 creep types, 1 tower type, no persistence
- **Turrest02**: Advanced mode - 6 creep types, 5 tower types, XP/stats persistence

Both extend `Game<T extends Player>` abstract class with shared map loading, player management, and broadcast mechanisms.

### Communication Pattern
WebSocket commands flow through a routing system:
```
Client → SocketHandler → CommandRouter → [Subject]Handler → [Topic]Listener → Game
```

Commands use subject/topic pairs (e.g., `GAME/PLACE_TOWER`). Server broadcasts updates via `ServerToClientCommand` subclasses.

### Game Loop
200ms tick rate (5 Hz). Each tick: spawn creeps → update positions → tower attacks → resource generation. Batched updates sent to clients to reduce network overhead.

### Key Directories
```
src/main/java/be/lefief/
├── game/                      # Game logic (modes, towers, creeps, maps)
│   ├── turrest01/            # Basic game mode
│   └── turrest02/            # Advanced game mode with persistence
├── sockets/                   # WebSocket handlers and command routing
│   ├── handlers/routing/     # CommandRouter for subject/topic dispatch
│   └── commands/             # Client/server command definitions
├── lobby/                     # Lobby management
├── repository/               # JPA repositories
└── service/                  # Business logic services

frontend/src/app/
├── core/services/            # SocketService, AuthService, etc.
├── features/game/            # Main game component (~2000 lines)
└── shared/models/            # TypeScript interfaces
```

### Adding New Features

**New Tower Type (Turrest02)**:
1. Add entry to `TowerDefinition` enum with stats (range, damage, cooldown, costs)
2. Frontend: Add to tower selection UI in `action-panel.component.ts`

**New Creep Type (Turrest02)**:
1. Add entry to `CreepType` enum with stats (speed, hp, cost, reward)
2. Frontend: Add to creep panel in `send-creeps-panel.component.ts`

**New Game Command**:
1. Create `[Topic]Handler` implementing `ClientToServerCommand`
2. Create `[Topic]Listener` to route to game handler
3. Create `[Topic]Response` for server-to-client if needed
4. Frontend: Add to `SocketService` message handling

### Database
- Development: H2 file database at `C:/data/turrest/db`
- Migrations: Flyway scripts in `src/main/resources/db/scripts/`
- Key tables: `USERPROFILE`, `PLAYER_STATS`, `MATCH_HISTORY`

### Testing
Backend uses JUnit 5 + Mockito. Key test files in `src/test/java/be/lefief/`. Frontend uses Vitest.

## Code Conventions

- Lombok `@Data`, `@RequiredArgsConstructor` for boilerplate reduction
- Game definitions (towers, creeps) are data-driven via enums
- Commands use Jackson for JSON serialization
- Frontend uses standalone Angular components with RxJS
- Prettier: 100 char line width, single quotes
