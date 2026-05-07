-- Tracks which rows have been successfully committed during a bulk WSP/ATR migration import.
-- Allows the ImportWspAtrMigrationFile process to resume from the last committed row
-- after a failure, rather than re-processing everything from the start.
--
-- To re-run a clean import for a given file, delete rows for that file:
--   DELETE FROM ZZ_WSP_ATR_Migration_Progress WHERE SourceFile = 'bulkupload.xlsx';
--
-- To wipe all progress:
--   TRUNCATE ZZ_WSP_ATR_Migration_Progress;

CREATE TABLE IF NOT EXISTS ZZ_WSP_ATR_Migration_Progress (
    AD_Client_ID  INTEGER      NOT NULL,
    SourceFile    VARCHAR(255) NOT NULL,
    TabName       VARCHAR(255) NOT NULL,
    LineNo        INTEGER      NOT NULL,
    ProcessedAt   TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT ZZ_WSP_ATR_Migration_Progress_pk
        PRIMARY KEY (AD_Client_ID, SourceFile, TabName, LineNo)
);

CREATE INDEX IF NOT EXISTS ZZ_WSP_ATR_Migration_Progress_idx
    ON ZZ_WSP_ATR_Migration_Progress (AD_Client_ID, SourceFile, TabName);
