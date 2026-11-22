#!/usr/bin/env bash
# Creates a logical PostgreSQL dump and a compressed snapshot of local file storage.
# Run from anywhere: bash scripts/backup.sh
set -euo pipefail
cd "$(dirname "$0")/.."

POSTGRES_DB="${POSTGRES_DB:-lifeos}"
POSTGRES_USER="${POSTGRES_USER:-lifeos}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
STORAGE_ROOT="${FILE_STORAGE_ROOT:-./docker-data/storage}"
RETENTION="${BACKUP_RETENTION:-7}"
TS="$(date -u +%Y%m%dT%H%M%SZ)"

mkdir -p "$BACKUP_DIR"

echo "[backup] database -> $BACKUP_DIR/lifeos-db-$TS.dump"
docker compose exec -T postgres pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc \
  > "$BACKUP_DIR/lifeos-db-$TS.dump"

if [ -d "$STORAGE_ROOT" ]; then
  echo "[backup] storage -> $BACKUP_DIR/lifeos-storage-$TS.tar.gz"
  tar -czf "$BACKUP_DIR/lifeos-storage-$TS.tar.gz" -C "$STORAGE_ROOT" .
else
  echo "[backup] storage root '$STORAGE_ROOT' not found; skipping storage snapshot"
fi

# Keep only the most recent dumps/storage snapshots.
find "$BACKUP_DIR" -maxdepth 1 -name 'lifeos-db-*.dump' -type f | sort | head -n -"$RETENTION" | xargs -r rm -f
find "$BACKUP_DIR" -maxdepth 1 -name 'lifeos-storage-*.tar.gz' -type f | sort | head -n -"$RETENTION" | xargs -r rm -f

echo "[backup] done"
