# Frontend Index

## Location
`C:\Users\timil\IdeaProjects\turrest-server-ai\frontend\`

## Key Files

### Services
- `src/app/core/services/socket.service.ts` - WebSocket connection
- `src/app/core/services/game.service.ts` - Game state management
- `src/app/core/services/lobby.service.ts` - Lobby operations
- `src/app/core/services/auth.service.ts` - Authentication

### Game Component
- `src/app/features/game/game.component.ts` - Main game canvas (~2000 lines)
- `src/app/features/game/game.component.html` - Game template
- `src/app/features/game/game.component.scss` - Game styles

### Game Sub-Components
- `components/action-panel/` - Tower/Building placement UI
- `components/send-creeps-panel/` - Creep sending UI
- `components/resource-bar/` - Wood/Stone/Gold display
- `components/ranklist/` - Player scoreboard
- `components/minimap/` - Map overview

### Models
- `src/app/shared/models/game.model.ts` - Tile, Creep, Tower interfaces
- `src/app/shared/models/socket-command.model.ts` - Command structure

## CRITICAL: Tower/Creep UI

The frontend currently has **hardcoded** tower and creep options. To display the new Turrest02 towers/creeps, the frontend must:

1. Read `towers` array from `FULL_MAP` response
2. Read `creeps` array from `FULL_MAP` response
3. Dynamically render buttons/options based on these arrays

### Backend sends (in FullMapResponse):
```json
{
  "towers": [
    {"id": 1, "name": "Basic Tower", "damage": 30, "range": 3.0, "costGold": 100, ...},
    {"id": 2, "name": "Sniper Tower", "damage": 80, "range": 5.0, "costGold": 200, ...},
    ...
  ],
  "creeps": [
    {"id": "GHOST", "speed": 30, "hitpoints": 50, "sendCostGold": 10, ...},
    {"id": "RUNNER", "speed": 20, "hitpoints": 25, "sendCostGold": 5, ...},
    ...
  ]
}
```

### Frontend needs to update:
1. `action-panel/` - Show all tower types from `towers` array
2. `send-creeps-panel/` - Show all creep types from `creeps` array

## Running Frontend
```bash
cd frontend
npm install
npm start
# Opens http://localhost:4200
```
