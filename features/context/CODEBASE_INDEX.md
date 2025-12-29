# Turrest Server AI - Codebase Index

## Quick Reference

- **Backend:** Spring Boot 2.7.18, Java 17, port 8081
- **Frontend:** Angular 21+, port 4200, located at `frontend/`
- **Database:** H2 (dev) at `C:/data/turrest/db`
- **WebSocket:** `ws://localhost:8081/ws/lobby`

---

## 1. PROJECT STRUCTURE

```
turrest-server-ai/
├── src/main/java/be/lefief/
│   ├── Main.java                    # Entry point
│   ├── config/                      # Spring configs
│   ├── controller/                  # REST endpoints
│   │   ├── AuthenticationController.java
│   │   └── LeaderboardController.java
│   ├── game/                        # Core game logic
│   │   ├── Game.java               # Abstract base
│   │   ├── Player.java             # Abstract player
│   │   ├── GameService.java        # Game lifecycle
│   │   ├── map/                    # Map system
│   │   ├── turrest01/              # Game mode 1
│   │   └── turrest02/              # Game mode 2
│   ├── lobby/                       # Lobby system
│   ├── repository/                  # Database access
│   │   ├── UserProfile*.java
│   │   └── turrest02/              # Stats persistence
│   ├── service/                     # Business logic
│   │   ├── lobby/
│   │   ├── userprofile/
│   │   └── turrest02/              # PersistentStatsService
│   ├── sockets/                     # WebSocket handling
│   │   ├── ClientSession.java
│   │   ├── SocketHandler.java
│   │   └── handlers/               # Command routing
│   └── util/                        # Utilities
├── src/main/resources/
│   ├── application.properties
│   ├── db/scripts/                  # Flyway migrations
│   └── levels/                      # Game maps
└── frontend/src/app/                # Angular frontend
    ├── core/services/              # Socket, Auth, Game services
    ├── features/
    │   ├── auth/                   # Login/Register
    │   ├── game/                   # Game canvas & UI
    │   └── lobby/                  # Lobby management
    └── shared/models/              # TypeScript models
```

---

## 2. TURREST01 vs TURREST02

| Feature | Turrest01 | Turrest02 |
|---------|-----------|-----------|
| Tower Types | 1 (Basic) | 5 (Basic, Sniper, Splash, Slow, Rapid) |
| Creep Types | 2 (Ghost, Troll) | 6 (+Runner, Tank, Healer, Swarm) |
| Persistent Stats | No | Yes (XP, Leaderboards) |
| Bean Names | Default | Prefixed with `turrest02` |

### Tower Definitions (Turrest02)

| Tower | Range | Damage | Cooldown | Cost (W/S/G) | Special |
|-------|-------|--------|----------|--------------|---------|
| BASIC | 3.0 | 30 | 1.0s | 80/80/100 | - |
| SNIPER | 5.0 | 80 | 3.0s | 60/150/200 | Long range |
| SPLASH | 2.5 | 20 | 1.5s | 120/120/150 | AoE (1 tile) |
| SLOW | 2.5 | 15 | 0.8s | 60/100/130 | 50% slow 2.5s |
| RAPID | 2.0 | 12 | 0.4s | 100/80/100 | Fast fire |

### Creep Definitions (Turrest02)

| Creep | Speed | HP | Send Cost | Kill Reward | Special |
|-------|-------|-----|-----------|-------------|---------|
| GHOST | 30 | 50 | 10G | 5G | - |
| TROLL | 25 | 250 | 30G | 15G | - |
| RUNNER | 20 | 25 | 5G | 3G | Very fast |
| TANK | 40 | 500 | 60G | 30G | Slow, tanky |
| HEALER | 30 | 80 | 25G | 10G | Heals nearby |
| SWARM | 25 | 15 | 8G | 2G | Spawns 5 |

---

## 3. KEY FILES BY FUNCTION

### Game Modes
- `game/turrest01/TurrestGameMode01.java` - Mode 1 game loop
- `game/turrest02/TurrestGameMode02.java` - Mode 2 game loop
- `game/turrest0X/Turrest0XPlayer.java` - Player state

### Towers
- `game/turrest02/tower/TowerDefinition.java` - All tower stats
- `game/turrest02/tower/TowerManager.java` - Targeting & firing
- `game/turrest02/tower/GenericTower.java` - Dynamic tower impl

### Creeps
- `game/turrest02/creep/CreepType.java` - All creep stats
- `game/turrest02/creep/CreepManager.java` - Spawning & movement
- `game/turrest02/creep/Creep.java` - Individual creep

### Commands (Backend → Frontend)
- `commands/FullMapResponse.java` - Initial game state + tower/creep defs
- `commands/TowerPlacedCommand.java` - Tower placed
- `commands/BatchedCreepUpdateCommand.java` - Creep positions
- `commands/BatchedTowerAttackCommand.java` - Tower attacks
- `commands/ResourceUpdateResponse.java` - Resource changes

### Handlers (Frontend → Backend)
- `handlers/PlaceTowerHandler.java` - Place tower request
- `handlers/PlaceBuildingHandler.java` - Place building request
- `handlers/SendCreepHandler.java` - Send creep request
- `handlers/Turrest02GameHandler.java` - All game actions

### Persistence (Turrest02 only)
- `repository/turrest02/PlayerStatsRepository.java`
- `repository/turrest02/PersistentPlayerStats.java`
- `service/turrest02/PersistentStatsService.java`

### Frontend Game
- `frontend/src/app/features/game/game.component.ts` - Main canvas (~2000 lines)
- `frontend/src/app/core/services/socket.service.ts` - WebSocket client

---

## 4. COMMAND FLOW

```
Frontend (Angular)
    ↓ socketService.sendCommand('GAME', 'PLACE_TOWER', {x, y, towerType})
WebSocket → ClientSession
    ↓ JSON deserialization
CommandRouter → routes by subject/topic
    ↓
PlaceTowerHandler.identify() → SecuredClientToServerCommand
    ↓
PlaceTowerListener.accept()
    ↓
Turrest02GameHandler.handlePlaceTower()
    ↓ validates, updates game state
game.broadcastToAllPlayers(new TowerPlacedCommand(...))
    ↓
Frontend receives → updates canvas
```

---

## 5. DATABASE SCHEMA

### USERPROFILE
```sql
id uuid PRIMARY KEY
name varchar(64) UNIQUE
registered_at timestamp
password varchar(64)
```

### PLAYER_STATS (Turrest02)
```sql
user_id uuid PRIMARY KEY REFERENCES USERPROFILE
total_games_played, total_wins, total_losses int
total_creeps_killed, total_creeps_sent bigint
total_gold_earned, total_gold_spent bigint
total_damage_dealt, total_damage_taken bigint
xp bigint
current_win_streak, best_win_streak int
```

### MATCH_HISTORY (Turrest02)
```sql
id uuid PRIMARY KEY
game_id uuid
user_id uuid REFERENCES USERPROFILE
player_number int
is_winner boolean
final_hp, creeps_killed, creeps_sent int
xp_earned int
played_at timestamp
```

---

## 6. IMPORTANT NOTES

### Bean Name Conflicts
Turrest02 handlers must have unique bean names to avoid conflicts with turrest01:
```java
@Component("turrest02PlaceTowerHandler")  // NOT just @Component
public class PlaceTowerHandler ...
```

Affected classes:
- GetStatsHandler, GetStatsListener
- PlaceBuildingHandler, PlaceBuildingListener
- PlaceTowerHandler, PlaceTowerListener
- SendCreepHandler, SendCreepListener

### Frontend Tower/Creep Display
The `FullMapResponse` now includes tower and creep definitions in the `towers` and `creeps` arrays. The frontend must read these to display all available options (not hardcode them).

### Game Tick Rate
- Backend: 200ms (5 Hz)
- Resource production: Every 5 ticks (1 second)

### Starting Resources
- Wood: 100, Stone: 100, Gold: 100
- Base production: +1/sec each

---

## 7. RUNNING THE PROJECT

### Backend
```bash
JAVA_HOME="/c/Users/timil/.jdks/corretto-17.0.16" \
"/c/Program Files/ApacheSoftwareFoundation/apache-maven-3.9.1/bin/mvn" \
spring-boot:run
```

### Frontend
```bash
cd frontend
npm start
```

### Tests
```bash
mvn test  # 72 tests
```

---

## 8. API ENDPOINTS

### REST
- `POST /login` - Authenticate
- `POST /register` - Create account
- `GET /api/leaderboard/xp` - Top players by XP
- `GET /api/leaderboard/wins` - Top players by wins
- `GET /api/leaderboard/player/{userId}` - Player stats

### WebSocket Topics (Client → Server)
- `LOBBY.CREATE_LOBBY`
- `LOBBY.JOIN_LOBBY`
- `LOBBY.START_GAME`
- `GAME.PLACE_TOWER`
- `GAME.PLACE_BUILDING`
- `GAME.SEND_CREEP`
- `GAME.GET_STATS`

### WebSocket Topics (Server → Client)
- `GAME.FULL_MAP`
- `GAME.TOWER_PLACED`
- `GAME.BUILDING_CHANGED`
- `GAME.CREEP_SPAWNED`
- `GAME.CREEP_UPDATE`
- `GAME.TOWER_ATTACK`
- `GAME.RESOURCE_UPDATE`
- `GAME.PLAYER_DAMAGE`
- `GAME.GAME_OVER`
