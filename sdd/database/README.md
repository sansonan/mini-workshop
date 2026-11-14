# Database

This directory is reserved for PostgreSQL database assets and SQL documentation.

Use it for reviewed, ordered schema migrations, reference data scripts where appropriate, data model documentation, local database setup guidance, and SQL notes. Keep persisted structures consistent with the approved data model and application specifications in `docs/`.

Document the selected migration tool and conventions before adding migrations. Consider constraints, indexes, data integrity, and the effects of applying or rolling back each change. Never commit credentials or production data. No schema or migration is included in this foundation.
