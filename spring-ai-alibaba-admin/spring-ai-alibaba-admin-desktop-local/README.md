# Spring AI Alibaba Admin Desktop Local

This module is the isolated backend boundary for desktop-local development.

Rules:

- Do not modify existing multi-tenant `/console/v1/*` controllers or services for desktop-local behavior.
- Use `/desktop/v1/*` for desktop-local APIs.
- Keep desktop-local persistence in `desktop_local_*` tables or a desktop-local profile table.
- Add shared behavior through additive adapters only when existing multi-tenant behavior remains unchanged.

Database scripts:

- `src/main/resources/db/desktop-local/schema-m1.sql`: MySQL-compatible desktop-local schema.
- `src/main/resources/db/desktop-local/schema-m1-sqlite.sql`: SQLite-compatible desktop-local schema.
