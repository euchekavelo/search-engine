DROP PROCEDURE IF EXISTS test ^;
CREATE PROCEDURE test()
BEGIN
    IF NOT EXISTS (
                    SELECT  *
                    FROM    information_schema.statistics
                    WHERE 	TABLE_SCHEMA = 'search_engine'
                            AND TABLE_NAME = 'page'
                            AND COLUMN_NAME = 'path'
                            AND INDEX_NAME = 'path_index'
                  ) THEN
        CREATE INDEX path_index ON page(path(50));
    END IF;
END ^;

CALL test ^;