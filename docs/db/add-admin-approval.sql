-- Reference only; not executed by this task.
-- Check INFORMATION_SCHEMA.COLUMNS first. Run only when all three columns are absent.
-- Normal application deployment currently uses Hibernate ddl-auto=update.
-- NULL is reserved for legacy accounts and preserves their existing access.
-- Public registration in the new application explicitly writes PENDING.
ALTER TABLE admin
    ADD COLUMN approval_status VARCHAR(20) NULL,
    ADD COLUMN approved_at DATETIME(6) NULL,
    ADD COLUMN approved_by_admin_id BIGINT NULL;

-- Do not set new PENDING accounts to NULL or roll back to code that ignores approval.
-- No update/backfill or destructive rollback is required for legacy accounts.
