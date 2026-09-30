# Studio Pack runtime

The runtime is implemented on the Studio Java backend branch. It is not a production
release or a replacement for Stack authorization. Pack data is platform-wide; this
version does not activate tenant-specific assets.

## Build and validate

Use the designated build checkout, Java 21, Maven and the existing isolated PostgreSQL
harness. The schema is owned by `protocol/` and included in the engine JAR at build time.
The CLI never starts Spring, queries business data, or invokes a model.

```bash
./build.sh package
./engine/tools/pack-cli validate /path/to/pack-source --strict
./engine/tools/pack-cli build /path/to/pack-source -o /new/path/pack.dtspack --strict
./engine/tools/liquibase-to-pack-templates /new/path/templates.json
```

Build output must not already exist or be inside the input directory. Every file in
the source must be declared by the manifest (except manifest and SHA256SUMS). Build
regenerates checksums in the archive; validate checks the committed source checksums.
The exporter creates its own disposable database and replays the original template
changesets in order, including the 031/032 fixes. Its fingerprint is migration evidence,
not an ownership permission supplied by uploaded Pack data.

CLI exits: 0 valid, 1 error, 2 warnings. `--strict` makes warnings an error. Input limits:
20 MiB compressed, 8 MiB per entry, 64 MiB expanded, 2,000 ZIP entries. Unsafe paths,
symlinks in source directories, duplicate entries, unsupported schema versions, hash
mismatches, duplicate domains and undeclared files are rejected. Validation is offline.
No archive entry is extracted to disk. Multipart buffering is configured in memory.

## Administration API

Every request requires a valid existing API key (`Authorization: Bearer ...`) and
`X-Admin-Secret` matching a nonempty, non-default `COPILOT_ADMIN_SECRET`. Caller-supplied
role/user headers do not grant Pack administration. This is the interim administration
contract; gateway-verified roles and tenant identity belong to BL-S.

| Method | Route | Behavior |
|---|---|---|
| POST | `/api/ai/packs` | Multipart `file`; 201 installed, 200 identical archive |
| GET | `/api/ai/packs` | Version metadata ordered by Pack and installation time |
| GET | `/api/ai/packs/{name}` | All installed versions; 404 if absent |
| POST | `/api/ai/packs/{name}/versions/{version}/activate` | Atomic switch; returns previous version and generation |
| POST | `/api/ai/packs/{name}/rollback` | Reactivate most recently superseded version |
| GET | `/api/ai/packs/{name}/versions/{version}/assets?kind=ontology` | Asset metadata and hashes |

A different archive under the same name/version is 409. Active version reactivation,
missing rollback target, cross-Pack asset/domain ownership and template ownership
conflicts are 409. Validation failures are 422; oversized content is 413. Error bodies
contain `code` and `errors`. No endpoint grants data-source access or executes uploaded SQL.

Install, activation and rollback record CloudEvents in `studio_audit_outbox`. Outbox
insertion failure rolls back the operation. Publishing this outbox to `dts.audit.v1`
remains BL-S integration work; rows are not evidence of Kafka delivery.

## Assets and refresh

`PackAssetResolver` reads active assets and generation in one repeatable-read snapshot.
Each instance checks within ten seconds. Registry readers rebuild their typed state
under a lock; errors fail the read instead of returning an empty governance rule set.
Source compatibility keys retain the full resource name, e.g.
`guardrails/governance/caliber-rules.v1.json` means kind `guardrails` and key
`governance/caliber-rules.v1.json`. The slash notation is documentation, not a second key.

`dts.studio.pack.fallback-classpath` defaults to true during migration. It applies only
before the first activation (generation zero). Registry errors and missing assets after
activation never silently use classpath content. Set it to false for Pack-only acceptance.
This flag alone does not remove other transitional domain code or historical SQL seeds.

The six historical semantic files contain five effective domains: `field-operations.json`
and `flowerbiz.json` both declare `flowerbiz`; the original loader retained the latter.
The PRS archive preserves that effective result with five ontology assets. The shadowed
file remains historical input; it is not renamed into a new domain.

## Template ownership and rollback

Migration 002 marks only the 57 exact historical seed records, using content fingerprints.
Rows whose content was edited retain manual ownership. Activation updates the relational
template projection in the same transaction as the Pack status, generation and audit.
Other Pack/manual records are protected by collision checks and conditional upsert.
Template matching observes generation changes instead of waiting for its five-minute TTL.
The source Pack version and logical data-source reference are stored with projected rows.

Use Pack rollback to revert an installed version. Database schema rollback has only been
tested on an empty registry; it is not a data-restoration procedure for a populated release.
Keep backups and follow the release workflow before a production migration.

## Validation lanes and dependencies

- `./build.sh verify`: both backend modules, disposable PostgreSQL 18.4, no business `.env`.
- `PackRuntimeSmokeIT`: explicit real HTTP/JPA/Liquibase test, requiring a prebuilt PRS
  archive and local `pgvector/pgvector:pg17`. It verifies authentication, upload idempotency,
  activation, no-classpath semantic equality, template question/SQL equivalence and audit.
- CI runs backend/validator tests. A submitted workflow is not proof of a remote CI run.
- JSON Schema validator 1.5.6 and Jackson YAML 2.18.3 use Apache 2.0 licenses. Jackson is
  pinned by the existing Spring Boot dependency BOM. No runtime network schema fetch is used.

Remaining integrations are explicit: verified identity/roles and audit publication (BL-S),
Stack QueryGateway before converting garden tools (BL-A/T11), service-bound action dispatch
(T12), DAP/UI contract generation and consumers (T15–T19), removal of transitional domain
code (BL-D), and Console/BFF delivery (BL-C). These are not delivered by installing a Pack.

## Action service references (subsequent 0.1.1 asset revision)

PRS 0.1.1 extracts its action into `actions/bad-debt-draft.json` and uses
`target.serviceRef=prs-legacy-adminapi`. Its paths and required permission are unchanged;
credentials and base URLs belong to `dts.studio.action.services.<ref>` deployment bindings.
The example overlay is `prs-stack/pack/studio-action-bindings.example.yaml`, outside the archive.

`ActionClient`/`HttpActionClient` replace the old client classes. Bindings support
`auth-mode: service-token` (`X-DTS-Service-Token`) and `bearer` (`Authorization: Bearer ...`),
with raw credentials supplied through deployment secrets. `forward-user` remains rejected
until BL-S supplies a verified delegated user credential. Do not reuse an arbitrary caller
Authorization header as a business-service credential.

The current user flow still confirms and creates a draft; it does not expose a new commit
endpoint. Draft methods must be POST. The generic transport supports other mutation methods
for future approved flows, but confirmation and authorization remain above the transport.
It uses the planned `X-DTS-User-Id`, `X-DTS-Service` and `X-DTS-Trace-Id` outbound names
for the current request actor, service and server-generated request ID; authenticated JWT
and tenant propagation are still BL-S work. The destination must enforce its own permissions.

Unbound targets produce `ACTION_TARGET_UNBOUND` in the draft result's `responseBody.code`.
Calls have a 5-second connection timeout, 30-second overall deadline and 1 MiB response cap.
Redirects and encoded/relative path escapes are rejected. There is no application retry.
Transport interruptions are `ACTION_STATUS_UNKNOWN`: a caller must reconcile the business
state instead of assuming a write failed. An idempotency header is sent, but exactly-once
behavior requires destination support and is not claimed. HTTP 2xx with an explicit business
failure (`success=false` or a non-200 `code`) is not reported as success.

Existing action audit records remain in use. CloudEvent action outbox delivery, verified
identity and a new formal-commit workflow are not supplied by this transport refactor.

## Answer provenance (2026-09-30)

Every new chat execution returns `packRefs: [{"type":"pack","name":"prs-flower","version":"0.1.1"}]`.
The same array is included in the SSE `done` event and in `trace.packRefs`, persisted with the
assistant message. Session replay reads that stored trace, never the registry's current ACTIVE
version. The offline schema is `protocol/pack-source-ref.v1.schema.json`.

These are consulted Pack dependencies, including catalogs used during routing and rules used
for validation; they are not citations proving that every asset contributed to the final text.
Only readers actually invoked contribute sources. A parsed catalog cache retains its original
Pack dependencies, so warm-cache requests have the same provenance as cold-cache requests.
Template sources come from each loaded row's immutable `source_pack_version_id`, not the
resolver's current generation. Missing ownership metadata fails rather than guessing a version.
Manual and pre-Pack legacy templates do not receive invented Pack references.

Collection starts inside `AgentExecutionService` on the executing thread and closes on success,
error, or interruption. Scope data is isolated between requests, nested calls, and worker
threads. Current ReAct tool calls run synchronously; future asynchronous Pack readers must
explicitly propagate and merge evidence before completing an answer. A scope does not pin
an entire conversation to one registry generation: if activation occurs during execution and
both versions are subsequently read, both references are retained in first-read order without
duplicates. Activation alone cannot relabel an answer that only read the older version.

The inherited REST `sourceRefs` string and SSE string array remain compatible. BL-A/T17 and
BL-C/T02 must adapt `packRefs` into the new Console's canonical structured `sourceRefs[]`,
using this schema as the engine source. This change does not freeze the whole Agent UI
protocol or change Console's frozen REST/SSE contract. New answers with no Pack reads use
an empty array; historical messages lacking provenance omit the new field.

Verification includes model-call activation races (mock model, real readers/execution/chat
persistence flow), cache hits, legacy fallback, template ownership, error cleanup, nested scope
and worker isolation. The full-runtime lane also checks real HTTP chat, SSE and session replay
against the installed PRS Pack in disposable PostgreSQL. It does not call a live model or
establish authenticated production business acceptance.
