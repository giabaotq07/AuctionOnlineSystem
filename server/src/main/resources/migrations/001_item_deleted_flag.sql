SET @has_deleted = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'items'
      AND column_name = 'deleted'
);

SET @sql = IF(
    @has_deleted = 0,
    'ALTER TABLE items ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE AFTER created_at',
    'SELECT ''items.deleted already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_status = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'items'
      AND column_name = 'status'
);

SET @sql = IF(
    @has_status = 1,
    'UPDATE items SET deleted = CASE WHEN status = ''DELETE'' THEN TRUE ELSE FALSE END',
    'SELECT ''items.status already absent'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @has_status = 1,
    'ALTER TABLE items DROP COLUMN status',
    'SELECT ''items.status already absent'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
