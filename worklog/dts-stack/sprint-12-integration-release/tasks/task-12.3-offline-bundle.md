# Task 12.3: Offline Bundle

**Sprint:** 12 — Integration | **Points:** 5 | **Status:** TODO

## Responsibility
- `dts bundle create` — package all images + charts + config into tarball
- `dts bundle install` — load images into local registry, apply Helm charts
- Include all middleware images
- Include embedded LLM images (optional)
- Verification checksums

## Steps
1. Implement `dts bundle create` → tests
2. Implement `dts bundle install` → tests
3. Test full offline installation on clean machine
4. Commit
