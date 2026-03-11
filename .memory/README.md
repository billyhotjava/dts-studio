# .memory/ — DTS Knowledge Memory System

AI 对话记忆与领域知识管理系统。

## Architecture

```
.memory/
├── conversations/           # Engram — 对话记忆 (SQLite)
│   └── engram.db            #   设计讨论、决策记录、问答历史
├── ontology/                # 结构化领域知识 (YAML/Markdown)
│   ├── types/               #   Ontology 类型定义 (ObjectType YAML)
│   ├── relationships/       #   关系定义 (Relationship YAML)
│   ├── patterns/            #   跨行业通用模式 (3 patterns defined)
│   │   ├── asset-inspection-issue-workorder.yaml
│   │   ├── metric-threshold-alert-action.yaml
│   │   └── hierarchy-aggregation.yaml
│   └── industry/            #   行业领域知识
│       ├── energy/          #     电力/能源 (9 objects, 6 metrics)
│       ├── research/        #     科研院所 (14 objects, 6 metrics)
│       ├── manufacturing/   #     制造业 (10 objects, 6 metrics)
│       └── _template/       #     新行业模板
├── decisions/               # 设计决策日志 (Markdown)
│   └── 2026-03-11-architecture.md
└── .venv/                   # Python venv for Memori (internal)
```

## Tools

| Tool | Purpose | Storage | Interface |
|------|---------|---------|-----------|
| **Engram** | Conversation memory & decisions | SQLite (conversations/engram.db) | CLI, MCP, HTTP |
| **Memori** | Ontology semantic indexing (future) | SQLite (ontology/index.db) | Python SDK |
| **Files** | Structured domain knowledge | YAML/Markdown (git-tracked) | Direct read |

## Usage

### Engram (conversation memory)
```bash
export ENGRAM_DATA_DIR=/opt/prod/dts/rdc/.memory/conversations

# Search memories
engram search "architecture" --project dts

# Save a memory
engram save "topic" "content" --project dts

# View recent context
engram context dts

# Start MCP server (for Claude Code integration)
engram mcp

# Stats
engram stats
```

### MCP Integration (Claude Code)
`.mcp.json` in project root configures Engram as MCP server.
Restart Claude Code to activate — then Engram tools are available natively.

### Ontology knowledge
```bash
# All knowledge files are human-readable YAML/Markdown
# Git-tracked for version control
# Can be imported into dts-ontology-store as seed data
```
