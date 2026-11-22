#!/usr/bin/env bash
# Restores a dump into an isolated *restore_test* database and verifies it.
# This script refuses to touch the production database name.
set -euo pipefail
cd "$(dirname "$0")/.."

BACKUP_FILE="${1:-}"
POSTGRES_USER="${POSTGRES_USER:-lifeos}"
TARGET_DB="${RESTORE_TEST_DB:-lifeos_restore_test}"

if [ -z "$BACKUP_FILE" ] || [ ! -f "$BACKUP_FILE" ]; then
  echo "usage: bash scripts/restore-drill.sh <path-to-backup.dump>" >&2
  exit 2
fi

case "$TARGET_DB" in
  *restore_test*) ;;
  *) echo "refusing to restore into '$TARGET_DB': target must contain 'restore_test'" >&2; exit 2 ;;
esac

read -r -p "This will replace database '$TARGET_DB'. Type 'yes' to continue: " confirmation
if [ "$confirmation" != "yes" ]; then
  echo "aborted"
  exit 1
fi

echo "[restore] recreating $TARGET_DB"
docker compose exec -T postgres dropdb -U "$POSTGRES_USER" --if-exists "$TARGET_DB"
docker compose exec -T postgres createdb -U "$POSTGRES_USER" "$TARGET_DB"

echo "[restore] loading $BACKUP_FILE"
docker compose exec -T postgres pg_restore -U "$POSTGRES_USER" -d "$TARGET_DB" --no-owner --no-privileges \
  < "$BACKUP_FILE"

echo "[restore] verification:"
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$TARGET_DB" -Atc \
  "select 'users=' || count(*) from users; select 'households=' || count(*) from households; select 'records=' || count(*) from records;"
echo "[restore] drill completed successfully"
