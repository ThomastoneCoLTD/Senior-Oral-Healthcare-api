-- Reference-only, scoped production repair for JWT refresh-token truncation.
-- Verify both columns are nullable VARCHAR(255), utf8mb4_0900_ai_ci,
-- default NULL, with no column comment/extra attributes or indexes, on InnoDB.
-- If already VARCHAR(2048), skip that statement. Reject unexpected metadata.
-- Existing token values and all authentication claims remain unchanged.
-- Use the managed RDS Secret; do not copy credentials into this file.
-- LOCK=NONE/INPLACE must succeed; never fall back to table-copy DDL.
-- A short metadata lock is still possible. Abort rather than waiting on traffic.
SET SESSION lock_wait_timeout = 5;

ALTER TABLE `admin`
    MODIFY COLUMN `admin_refresh_token` VARCHAR(2048)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
    ALGORITHM=INPLACE, LOCK=NONE;

ALTER TABLE `user`
    MODIFY COLUMN `user_refresh_token` VARCHAR(2048)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
    ALGORITHM=INPLACE, LOCK=NONE;

-- Verify information_schema.columns reports both capacities as 2048.
-- Deploy matching @Column(length = 2048) mappings so startup schema update
-- retains this capacity. Keep the expanded columns during any rollback.
