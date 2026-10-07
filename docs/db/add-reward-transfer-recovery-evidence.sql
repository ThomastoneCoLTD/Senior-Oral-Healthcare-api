-- Reference only; not executed by this task.
-- Current production deployment uses Hibernate ddl-auto=update.
-- Confirm that the additive table exists before deploying the recovery frontend.
CREATE TABLE IF NOT EXISTS reward_transfer_recovery_evidence (
    fact_hash VARCHAR(100) NOT NULL,
    transaction_id BIGINT NOT NULL,
    admin_id BIGINT NULL,
    recovered_at DATETIME(6) NOT NULL,
    block_height BIGINT NOT NULL,
    PRIMARY KEY (fact_hash)
);
-- admin_id is populated for manual super-admin recovery and NULL for automatic completion.
-- Receipt claims survive reward/user reset or deletion; no cascade foreign keys are added.
-- Retain this table and its records on rollback to prevent consuming the same receipt again.
