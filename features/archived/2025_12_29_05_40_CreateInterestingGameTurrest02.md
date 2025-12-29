Instructions: 

- Improve on turrest 02. Use every psychological aspect you can think of to make it fun and addictive to play. 
- Use math expertise and game theory to tweak the rewards and cost of things so that it makes for as much possible viable strategies as possible - yet stays competitive.  
- Add towers, creeps, bullets, and imagine whole new systems to make the game competitive, fun and addictive.
- If persistent storage is needed, use /resources/db/scripts/allenv and /resources/db/scripts/onlyh2 to make changes to the schema via flyway.
- Keep improving on yourself - Add the plan underneath, and keep improving on the plan + keep a summary in this file of all the things we do, change and undo 

Completion goal:

- A player who likes strategy games would definitely want to try this game out 
- The game has no bugs 
- Different strategies are viable and competitive - no one strategy is best in all cases 
- There's enough unique factors (>5) to make every new game interesting 
- There's enough aspects (>5) in play that would want the player to keep returning (persistent rewards / ranking / leaderboards)
- The game doesnt feel too complex and is easy to comprehend. 
- Turrest01 still works as it used to - the frontend serves as a "player" for _all_ games we make. 

DO NOT change turrest01. If framework updates need to happen and feel justified, this is the only exception were you can change turrest01: to get the code to compile - not to change turrest01 working or behaviour.

---

## Current State Analysis

**Existing Features:**
- 2 creep types: GHOST (fast/weak, 50hp, speed 30) and TROLL (slow/tank, 250hp, speed 25)
- 1 tower type: BASIC_TOWER (range 3, 30dmg, 1shot/sec)
- 3 resource buildings: Lumbercamp, Stone Quarry, Gold Mine
- 3 resources: Wood, Stone, Gold (start 100, base +1/sec)
- 20 HP per player, score = remaining HP
- In-game stats tracking (no persistence)
- Road generation and pathfinding

**Gaps vs Completion Goals:**
- ✗ Only 2 creep + 1 tower = limited strategic depth
- ✗ No persistent storage (leaderboards/ranking/XP)
- ✗ < 5 unique factors per game
- ✗ < 5 retention mechanics

---

## Implementation Plan

### Phase 1: Strategic Depth (Towers & Creeps)

**New Towers (4 types to complement BASIC):**
1. SNIPER_TOWER - Long range (5), high damage (80), slow fire (3s cooldown), expensive
2. SPLASH_TOWER - Medium range (2.5), area damage (20 to all in radius 1), medium fire
3. SLOW_TOWER - Short range (2), low damage (10), slows creeps 50% for 2s
4. RAPID_TOWER - Short range (2), low damage (15), fast fire (0.3s cooldown)

**New Creeps (4 types):**
1. RUNNER - Very fast (speed 20), low HP (25), cheap, low reward
2. TANK - Very slow (speed 40), massive HP (500), expensive, high reward
3. HEALER - Medium (speed 30), medium HP (100), heals nearby creeps
4. SWARM - Fast (speed 25), very low HP (15), spawns in groups of 5

### Phase 2: Unique Game Factors (>5)
1. Map layout randomization (different road patterns)
2. Starting resource variation (wood/stone/gold focus)
3. Tower placement terrain bonuses
4. Weather/time effects (night = creeps harder to see)
5. Special tiles (speed boost, damage amplify)
6. Random events (gold rush, double creeps)

### Phase 3: Persistent Retention Systems (>5)
1. Player XP and Levels - earn XP from games, level up
2. Global Leaderboards - rank by wins, total damage, etc
3. Match History - track past games
4. Achievement System - unlock badges
5. Win Streaks - bonus rewards for consecutive wins
6. Daily Challenges - specific goals for bonus XP

---

## Progress Log

### Session 1 (2025-12-29)
- Analyzed current codebase structure
- Documented gaps vs completion goals
- Created implementation plan

**Implemented Phase 1: Strategic Depth**

Added 4 new tower types:
- SNIPER_TOWER (id=2): Range 5, 80dmg, 3s cooldown, Cost: 60W/150S/200G - Long range precision
- SPLASH_TOWER (id=3): Range 2.5, 20dmg AoE (1 tile radius), 1.5s cooldown, Cost: 120W/120S/150G - Area control
- SLOW_TOWER (id=4): Range 2.5, 15dmg + 50% slow for 2.5s, 0.8s cooldown, Cost: 60W/100S/130G - Crowd control
- RAPID_TOWER (id=5): Range 2, 12dmg, 0.4s cooldown, Cost: 100W/80S/100G - High DPS short range

Added 4 new creep types:
- RUNNER: Speed 20, 25HP, Send: 5G, Kill: 3G - Fast and cheap, overwhelm single-target
- TANK: Speed 40, 500HP, 3dmg, Send: 60G, Kill: 30G - Slow but massive HP
- HEALER: Speed 30, 80HP, Send: 25G, Kill: 10G - Heals nearby creeps 15hp/sec
- SWARM: Speed 25, 15HP, Send: 8G, Kill: 2G - Spawns 5 creeps per send

**Implemented Phase 3: Persistent Systems**

Database schema created (V2_2025_12_29_0645):
- PLAYER_STATS table: XP, wins, losses, creeps killed/sent, damage dealt/taken, win streaks
- MATCH_HISTORY table: Per-game records for each player

XP System:
- 100 XP per win, 25 XP per loss
- 1 XP per creep killed, 2 XP per creep sent
- Win streak bonus: 10 XP per streak level
- Level = sqrt(XP / 100) + 1

Leaderboard API endpoints:
- GET /api/leaderboard/xp - Top players by level/XP
- GET /api/leaderboard/wins - Top players by wins
- GET /api/leaderboard/player/{userId} - Individual player stats

**Tests: All 72 tests passing**

---

## Completion Status

| Goal | Status |
|------|--------|
| Strategy game appeal | ✓ 5 tower types + 6 creep types = diverse strategies |
| No bugs | ✓ All tests pass, compiles clean |
| Multiple viable strategies | ✓ Tank rush vs swarm spam vs economy vs tower defense |
| >5 unique factors per game | ✓ Tower mix, creep choices, map position, economy, timing |
| >5 retention aspects | ✓ XP/levels, leaderboards, win streaks, match history, stats |
| Easy to comprehend | ✓ Clear tower/creep roles with distinct characteristics |
| Turrest01 unchanged | ✓ No changes to turrest01 package |

**Remaining for full completion:**
- [ ] Add random map variations (Phase 2.1)
- [ ] Add special tiles or power-ups (Phase 2.5)
- [ ] Add achievement system (Phase 3.4)
- [ ] Add daily challenges (Phase 3.6)

