# Feature Proposals & Improvements

Potential improvements identified during codebase analysis.

## High Priority

### Game State Persistence
Currently games are lost on server restart. Implement game state serialization to allow:
- Resume interrupted games
- Replay system for post-game analysis
- Spectator mode joining mid-game

### Input Validation & Anti-Cheat
Client commands are trusted implicitly. Add server-side validation:
- Validate tower placement (sufficient resources, valid tile)
- Validate creep sending (cooldowns, costs)
- Rate limiting on command submissions
- Sanity checks on command timing

### Error Recovery & Reconnection
Improve handling of disconnected players:
- Grace period before removing player from game
- State sync on reconnection
- Handle partial message delivery

## Medium Priority

### Player Matchmaking
Replace manual lobby system with:
- Skill-based matchmaking using ELO/MMR
- Quick match queue
- Ranked vs casual modes

### AI Opponents
Single-player and practice modes:
- Bot players with configurable difficulty
- Tutorial mode with guided gameplay
- Training grounds for testing strategies

### Message Queuing
Commands processed sequentially per game. Consider:
- Command queue with priority levels
- Async processing for non-critical updates
- Backpressure handling for slow clients

## Low Priority / Nice-to-Have

### Database Optimizations
- Connection pooling configuration (HikariCP tuning)
- Read replicas for leaderboard queries
- Batch writes for match history

### Procedural Map Generation
Current maps are static `.level` files. Add:
- Random map generation with seed
- Map editor for custom levels
- Per-match random elements (resource node placement)

### Tournament System
- Bracket-based tournaments
- Scheduled matches
- Prize/reward distribution

### Observability
- Metrics export (Prometheus/Micrometer)
- Structured logging with correlation IDs
- Game telemetry for balance analysis

### Code Quality
- WebSocket integration tests (end-to-end game flow)
- Load testing for concurrent games
- Remove legacy TCP socket code (`SocketController.java`)
- Extract game state from `GameComponent` (~2000 lines) into dedicated services
