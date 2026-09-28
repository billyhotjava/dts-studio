# Legacy deployment reference

These files preserve the source Copilot deployment history and pending build/image
baseline changes. They are not the Studio deployment entry point: paths, service
names, credentials, and the removed webapp still follow the old repository layout.
Do not run these scripts or Compose files from Studio.

Use the repository root `build.sh` for backend verification and packaging. Studio's
supported release deployment is owned by RDC Sprint 5 F7/T25 under ADR-014 (K8s),
and remains pending. Keeping this reference does not provide a Compose fallback.
