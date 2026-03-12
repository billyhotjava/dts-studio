# Task 1.3: Python Monorepo — Workspace & dts-common-py

**Sprint:** 1 — Foundation
**Points:** 3
**Status:** TODO

## Files

- Create: `dts-stack/ai/pyproject.toml` (workspace root)
- Create: `dts-stack/ai/dts-common-py/pyproject.toml`
- Create: `dts-stack/ai/dts-common-py/src/dts_common/__init__.py`
- Create: `dts-stack/ai/dts-common-py/src/dts_common/config.py`
- Create: `dts-stack/ai/dts-common-py/src/dts_common/grpc_utils.py`
- Create: `dts-stack/ai/dts-common-py/src/dts_common/kafka_utils.py`
- Create: `dts-stack/ai/dts-common-py/src/dts_common/security.py`
- Test: `dts-stack/ai/dts-common-py/tests/test_config.py`
- Test: `dts-stack/ai/dts-common-py/tests/test_security.py`

## Step 1: Create workspace pyproject.toml

```toml
[project]
name = "dts-ai-workspace"
version = "3.0.0"
requires-python = ">=3.12"

[tool.uv.workspace]
members = ["dts-common-py", "dts-agent", "dts-intent-engine",
           "dts-ontology-engine", "dts-data-connector", "dts-data-quality",
           "dts-query-ai", "dts-ai-eval", "dts-observability", "dts-scheduler"]

[tool.ruff]
line-length = 100
target-version = "py312"

[tool.ruff.lint]
select = ["E", "F", "I", "N", "UP", "ASYNC"]
```

## Step 2: Create dts-common-py shared library

```python
# security.py
from dataclasses import dataclass
from uuid import uuid4

@dataclass(frozen=True)
class RequestContext:
    user_id: str
    tenant_id: str
    trace_id: str
    roles: list[str]
    permissions: list[str]

def extract_context(grpc_context) -> RequestContext:
    metadata = dict(grpc_context.invocation_metadata())
    jwt_token = metadata.get("authorization", "")
    claims = decode_jwt(jwt_token)
    return RequestContext(
        user_id=claims["sub"],
        tenant_id=claims["tenant_id"],
        trace_id=metadata.get("x-trace-id", str(uuid4())),
        roles=claims.get("roles", []),
        permissions=claims.get("permissions", []),
    )
```

## Step 3: Write tests

Run: `cd dts-stack/ai && uv sync && uv run pytest dts-common-py/tests/ -v`
Expected: PASS

## Step 4: Verify ruff

Run: `uv run ruff check .`
Expected: No errors

## Step 5: Commit

```bash
git add dts-stack/ai/
git commit -m "feat(s1): initialize Python AI workspace with dts-common-py"
```
