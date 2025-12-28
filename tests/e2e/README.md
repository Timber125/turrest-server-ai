# E2E Testing with Playwright MCP

This directory contains end-to-end test workflows that can be executed using Claude with Playwright MCP tools.

## Directory Structure

```
tests/e2e/
├── README.md           # This file
└── workflows/          # JSON workflow definitions
    └── multiplayer-game-flow.json
```

## Test Accounts

| Username | Password | Purpose |
|----------|----------|---------|
| Timber   | password | Main test account |
| NPC1     | npc1     | Multiplayer testing |
| NPC2     | npc2     | Multiplayer testing |

## Running Tests

To execute a test workflow, ask Claude:

> "Execute the E2E test workflow: multiplayer-game-flow"

Claude will:
1. Read the workflow JSON file
2. Execute each step using Playwright MCP browser tools
3. Report success/failure for each step
4. Take screenshots at key points

## Workflow JSON Schema

```json
{
  "name": "Test Name",
  "description": "What this test validates",
  "prerequisites": {
    "backend": "http://localhost:8081",
    "frontend": "http://localhost:4200"
  },
  "contexts": ["context1", "context2"],
  "steps": [
    {
      "id": "step-1",
      "context": "context1",
      "action": "navigate|click|type|fill|waitFor|screenshot|evaluate",
      "description": "Human readable step description",
      ...action-specific params
    }
  ]
}
```

## Available Actions

| Action | Parameters | Description |
|--------|------------|-------------|
| `navigate` | `url` | Navigate to a URL |
| `click` | `ref`, `element` | Click an element |
| `type` | `ref`, `element`, `text` | Type text into an element |
| `fill` | `fields[]` | Fill multiple form fields |
| `waitFor` | `text` or `time` | Wait for text or duration |
| `screenshot` | `filename` | Take a screenshot |
| `evaluate` | `function` | Execute JavaScript |
| `snapshot` | - | Get accessibility snapshot |

## Notes

- Tests use sessionStorage for authentication (tab-isolated)
- Canvas interactions require coordinate calculation via `evaluate`
- Multi-tab tests use `browser_tabs` to switch contexts
