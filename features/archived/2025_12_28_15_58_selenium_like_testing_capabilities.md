Instructions:

- We fixed all session issues and sharing tab issues. There's a few accounts we can use for testing:
1. Timber:password
2. NPC1:npc1
3. NPC2:npc2

I'd like you to give yourself testing capabilities: selenium test, MCP orchestration, but important: backed by a written workflow so we can write automatic tests around it.
Research the best options and suggest what you would propose.

Initially, i want you to:
- Login as NPC1 on tab 1
- Login as NPC2 on tab 2
- NPC1 creates lobby
- NPC2 joins lobby
- NPC1 & NPC2 ready up
- NPC1 starts game
- NPC1 builds a lumbercamp on a forest tile
- NPC2 builds a stone quarry on a rocky tile
- Wait until one player wins
- Logout from NPC1
- Logout from NPC2
- End test


---

## Research & Recommendation

### Options Evaluated

| Option | Pros | Cons |
|--------|------|------|
| **Playwright MCP** | Already available, multi-tab support, modern API, accessible snapshots | Limited to MCP tool interface |
| **Selenium WebDriver** | Industry standard, wide language support | Heavy setup, older API, slower |
| **Cypress** | Great DX, built-in waiting | Poor multi-tab support, single domain |
| **Playwright (standalone)** | Best multi-context support, fast | Requires separate test runner setup |

### Recommendation: **Playwright MCP + JSON Workflow Files**

**Why Playwright MCP:**
1. ✅ Already available in this environment via MCP tools
2. ✅ Excellent multi-tab support via `browser_tabs` tool
3. ✅ Accessibility snapshots for reliable element selection
4. ✅ Built-in waiting mechanisms
5. ✅ Can take screenshots for debugging
6. ✅ No additional installation required

**Workflow File Format:**
- JSON-based test definitions stored in `tests/e2e/workflows/`
- Each workflow describes steps, selectors, and assertions
- Claude can execute workflows by reading and interpreting them
- Human-readable and version-controllable

---

## Implementation Plan

### Phase 1: Directory Structure & Workflow Format

**Create test infrastructure:**
```
tests/
└── e2e/
    ├── workflows/
    │   └── multiplayer-game-flow.json
    └── README.md
```

**Workflow JSON Schema:**
```json
{
  "name": "Test Name",
  "description": "What this test validates",
  "contexts": ["npc1", "npc2"],
  "steps": [
    {
      "context": "npc1",
      "action": "navigate",
      "url": "http://localhost:4200"
    },
    {
      "context": "npc1",
      "action": "fill",
      "selector": "input[name=username]",
      "value": "NPC1"
    },
    {
      "context": "npc1",
      "action": "click",
      "selector": "button:has-text('Login')"
    },
    {
      "context": "npc1",
      "action": "waitFor",
      "text": "Welcome"
    }
  ]
}
```

### Phase 2: Implement Initial Test Workflow

**File:** `tests/e2e/workflows/multiplayer-game-flow.json`

**Steps to implement:**

1. **Setup Phase**
   - Open Tab 1, navigate to app
   - Open Tab 2, navigate to app

2. **Login Phase**
   - Tab 1: Login as NPC1 (username: NPC1, password: npc1)
   - Tab 2: Login as NPC2 (username: NPC2, password: npc2)
   - Wait for lobby list to appear on both tabs

3. **Lobby Phase**
   - Tab 1: Click "Create Lobby"
   - Tab 1: Wait for lobby room to load
   - Tab 2: Click on the lobby created by NPC1
   - Tab 2: Click "Join"
   - Tab 1: Click "Ready"
   - Tab 2: Click "Ready"
   - Tab 1: Click "Start Game"

4. **Game Phase**
   - Wait for countdown to finish (5 seconds)
   - Wait for map to load
   - Tab 1: Find a forest tile, click it, select "Lumber Camp", place building
   - Tab 2: Find a rocky tile, click it, select "Quarry", place building
   - Wait for game over condition (one player's HP reaches 0)

5. **Cleanup Phase**
   - Tab 1: Click "Back to Lobby" (if game over shown)
   - Tab 1: Click user menu → Logout
   - Tab 2: Click "Back to Lobby" (if game over shown)
   - Tab 2: Click user menu → Logout
   - Close both tabs

### Phase 3: Execution Approach

**Manual Execution via Claude:**
- Claude reads the workflow JSON
- Executes each step using Playwright MCP tools
- Reports success/failure for each step
- Takes screenshots at key points

**Key Playwright MCP Tools to Use:**
- `browser_tabs` - Create/switch between tabs
- `browser_navigate` - Go to URLs
- `browser_snapshot` - Get accessibility tree for element refs
- `browser_click` - Click elements
- `browser_type` - Type into inputs
- `browser_fill_form` - Fill multiple form fields
- `browser_wait_for` - Wait for text/conditions
- `browser_take_screenshot` - Capture state for debugging

### Phase 4: Challenges & Solutions

| Challenge | Solution |
|-----------|----------|
| Canvas-based game (no DOM elements) | Use coordinate-based clicks after identifying tile positions from game state |
| Finding specific tile types | Read game component's tile data or use visual inspection of canvas |
| Multi-tab state sync | Use `browser_tabs` to switch contexts, maintain tab index mapping |
| Timing issues | Liberal use of `browser_wait_for` with appropriate timeouts |
| Session isolation | Each tab has separate sessionStorage (already implemented) |

### Phase 5: Canvas Interaction Strategy

Since the game uses HTML5 Canvas, we need special handling:

1. **For tile selection:** Calculate pixel coordinates based on:
   - Tile position (x, y) from map data
   - Tile size (32px base * zoom)
   - Camera offset

2. **Approach:**
   - Use `browser_evaluate` to query game component state
   - Or use `browser_snapshot` to find canvas element, then calculate click coords
   - Execute click at calculated canvas coordinates

---

## Files to Create

1. `tests/e2e/README.md` - Documentation for running E2E tests
2. `tests/e2e/workflows/multiplayer-game-flow.json` - The initial test workflow

## Execution Command

To run a test, user would say:
> "Execute the E2E test workflow: multiplayer-game-flow"

Claude would then:
1. Read the workflow file
2. Execute each step using Playwright MCP
3. Report results with screenshots

---

## Implementation Order

1. Create directory structure and README
2. Create the multiplayer-game-flow.json workflow file
3. Execute the test manually to validate
4. Refine selectors and waits based on actual behavior
5. Document any issues encountered

