# DTS Studio

DTS Studio is the AI brain of DTS: model access, agent orchestration, conversational
analysis, tools, and the evolving AppPack runtime. DTS Stack owns the lakehouse
platform, governed data execution, metrics, and BI. Industry applications and
assets belong to DTS App Stack. DTS Infra owns installation and operations.

## Current implementation

The first refactoring slice imports the existing Java 21 / Spring Boot 3.4.5
Copilot backend with its history. Java packages, Maven artifact IDs, API paths,
and application behavior are preserved during this migration.

- `engine/engine-ai/`: imported AI backend, including existing LLM, agent, query,
  and retrieval capabilities. Domain and data-access separation is still pending.
- `engine/engine-analytics/`: transitional analytics backend for baseline regression;
  this is not a decision to retain a second BI platform inside Studio.
- `engine/worklog-history/`: historical Copilot planning and evidence.
- `engine/deploy/legacy/`: legacy deployment references; not executable Studio instructions.
- `.rules/`, `.skills/`, `.memory/`: inherited rules, capability definitions, and knowledge.

The old Copilot webapp is not imported. The new Console and BFF are separate planned
work under RDC F6 / BL-C. Stack and App Stack are sibling repositories, not nested submodules.

## Build and test

Requires Java 21, Maven, Python 3, Docker, and a local `postgres:18.4` test image.
`STUDIO_TEST_POSTGRES_IMAGE` can select another preloaded PostgreSQL 18 image. From this repository or any working directory:

```bash
./build.sh verify
./build.sh package
```

Both commands run backend tests unless the caller explicitly supplies Maven skip flags.
Builds do not load `.env` or invoke the old webapp. The test harness creates an
isolated PostgreSQL container on a random loopback port, overrides inherited
`PG_*` settings, and removes only that container on exit. Its data uses tmpfs; no
business database or existing service is modified. Test images are never pulled implicitly.
Build/test in the designated build checkout; current source and build SHAs must match.
The build checkout for this migration is `/data/dts-studio`.

Runtime credentials must be supplied externally. No source `.env` is imported.
K8s charts and offline delivery remain owned by RDC F7/T25 under ADR-014; they are
not delivered by this source import, and legacy Compose is not a release fallback.

## Product boundaries and implementation status

Studio proposes and orchestrates; Stack must enforce data access and own canonical
metric definitions. The imported JDBC tools are transitional code awaiting the
BL-S / BL-D refactoring, not the final data boundary. Pack externalization is BL-A.
Backend build and test success does not establish authenticated business acceptance.
The first joint acceptance scenario remains PRS in-operation project analysis.

Current plans, decisions, and acceptance evidence are maintained in
`dts-rdc/worklog/v1.0.0/`. Start with Sprint 5 F1 and its linked Features. Older
Python/25-service plans in inherited rules and historical worklogs are not current
implementation instructions. Preserve the five iron laws: human override,
authenticated access, authorized data, traceable operations, and tested APIs.

## License

Proprietary. All rights reserved.
