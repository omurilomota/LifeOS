# Backup & restore runbook

PostgreSQL is the system of record. Documents live in the storage root (`FILE_STORAGE_ROOT`). Both must be backed up, encrypted and restorable.

## Targets (proposed — pending owner confirmation)

| Metric | Target | Notes |
|---|---|---|
| RPO | 1 hour | Logical dump every hour in production; PITR reduces this to minutes |
| RTO | 4 hours | Time to restore DB + storage into an empty environment |
| Retention | 7 daily + 4 weekly dumps | Configurable via `BACKUP_RETENTION` |
| Drill frequency | Monthly in staging, quarterly in production-like environment | Evidence recorded in an ops issue |

These targets are a conservative proposal for a personal/family platform. Confirm or adjust before production rollout.

## What is backed up

1. **Database** — `pg_dump -Fc` (custom, compressed, restorable with `pg_restore`).
2. **Documents** — `tar.gz` snapshot of `FILE_STORAGE_ROOT`; in production prefer S3-compatible bucket versioning + lifecycle rules instead of local tarballs.

Backups contain personal data: encrypt at rest, restrict access to the ops role, never commit to git (`backups/` is gitignored), and never place them in a public web root.

## Local / staging backup

```bash
docker compose up -d postgres
bash scripts/backup.sh
```

Outputs `backups/lifeos-db-<UTC timestamp>.dump` and (when the storage root exists) `backups/lifeos-storage-<UTC timestamp>.tar.gz`. Old artifacts are pruned to `BACKUP_RETENTION` (default 7).

Environment variables: `POSTGRES_DB`, `POSTGRES_USER`, `BACKUP_DIR`, `FILE_STORAGE_ROOT`, `BACKUP_RETENTION`.

## Production database strategy

- Prefer **managed PostgreSQL PITR** (continuous WAL archiving) as the primary mechanism.
- Keep logical dumps via `pg_dump` as a portable secondary copy, stored in a separate account/region from the database.
- Schedule hourly dumps with the platform scheduler (cron/K8s CronJob); alert on failures.
- Verify backups are restorable automatically — an unverified backup is not a backup.

## Restore drill (isolated, non-destructive)

`scripts/restore-drill.sh` restores into a database whose name **must** contain `restore_test`; it refuses any other target, asks for explicit confirmation, and prints row counts after restore.

```bash
bash scripts/restore-drill.sh backups/lifeos-db-<timestamp>.dump
# type 'yes' when prompted
```

Successful drill output shows counts for `users`, `households` and `records`. Record the timestamp, source dump and counts in an ops issue. Drop the temporary database afterwards:

```bash
docker compose exec -T postgres dropdb -U lifeos --if-exists lifeos_restore_test
```

## Production restore (destructive — requires approval)

1. Freeze writes (maintenance mode) and announce the window.
2. Take a final dump of the damaged/current database for forensics.
3. Provision an empty database and restore with `pg_restore --no-owner --no-privileges`.
4. Restore the storage snapshot / re-enable bucket versioning to the matching point in time.
5. Run `mvn test` + the E2E smoke gate against the restored environment.
6. Verify household scoping and a sample of documents before reopening writes.
7. Document the incident, root cause and any data loss.

## Access control checklist

- [ ] Backup credentials are scoped to read/restore only, stored in a secret manager.
- [ ] Backups encrypted at rest and in transit; keys rotated periodically.
- [ ] Production backups isolated from application credentials and from public buckets.
- [ ] Restore drill performed and evidence recorded.
- [ ] RPO/RTO targets confirmed by the owner.
