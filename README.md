# MindMap — Personal Knowledge & Learning Discovery Platform

**"Learn from anything. Connect everything. Remember what matters."**

A JavaFX desktop application that turns information from movies, books, articles,
courses, projects, and conversations into an interconnected personal knowledge graph.

---

## Requirements

- **Java 17** or newer (JDK, not JRE)
- **Maven 3.9+**
- Internet connection on first run (Maven downloads dependencies)

Verify your setup:

    java -version
    mvn -version

---

## Run the application

From the project root:

    mvn clean javafx:run

The first launch will:

1. Create the app data directory
2. Create the SQLite database and all tables
3. Open a bootstrap window confirming Phase 1 is complete

---

## Data directory locations

| OS      | Path |
|---------|------|
| Windows | `%APPDATA%\MindMap\` |
| macOS   | `~/Library/Application Support/MindMap/` |
| Linux   | `~/.MindMap/` |

Inside you will find `mindmap.db` (SQLite), plus `backups/` and `attachments/`.

---

## Optional environment variables

| Variable                | Purpose                          | Default                          |
|-------------------------|----------------------------------|----------------------------------|
| `MINDMAP_AI_KEY`        | AI provider API key (Phase 9)    | *(empty — AI disabled)*          |
| `MINDMAP_AI_BASE_URL`   | AI provider base URL             | `https://api.openai.com/v1`      |
| `MINDMAP_AI_MODEL`      | Model name                       | `gpt-4o-mini`                    |

Set them before running, e.g. on Windows:

    set MINDMAP_AI_KEY=sk-...
    mvn javafx:run

On macOS/Linux:

    export MINDMAP_AI_KEY=sk-...
    mvn javafx:run

---

## Development phases

| Phase | Status | Description |
|-------|--------|-------------|
| 1 | ✅ | Project setup + database |
| 2 | ⏳ | JavaFX UI skeleton |
| 3 | ⏳ | (merged with Phase 1) |
| 4 | ⏳ | Authentication |
| … | ⏳ | see architecture doc |

---

## Reset the database

Delete the database file to start fresh (⚠ deletes all local data):

- Windows: `del "%APPDATA%\MindMap\mindmap.db"`
- macOS/Linux: `rm ~/.MindMap/mindmap.db` *(adjust for macOS path)*

Then run again — the schema will be recreated automatically.