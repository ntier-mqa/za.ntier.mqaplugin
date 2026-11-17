-- Move C_City rows for postal codes 0400 and 0407 (Hammanskraal) from
-- North West to Gauteng. Root cause: ImportCityFromCSV.java's
-- getProvinceFromZip() buckets zip range 300-499 as "North West", which
-- incorrectly swept in the 04xx Hammanskraal codes. That code-level mapping
-- is NOT fixed by this script -- only the existing data is.
--
-- Table: adempiere.c_city (C_Region_ID -> adempiere.c_region, province by Name)

BEGIN;

-- Step 1: full backup of the table, taken before any changes
DROP TABLE IF EXISTS adempiere.t_c_city_postal_0400_0407_backup;

CREATE TABLE adempiere.t_c_city_postal_0400_0407_backup AS
SELECT * FROM adempiere.c_city;

SELECT count(*) AS backup_row_count FROM adempiere.t_c_city_postal_0400_0407_backup;

-- Step 2: preview the rows that will be moved
SELECT c.ad_client_id ,c.c_city_id, c.name, c.postal, r.name AS current_region
FROM adempiere.c_city c
JOIN adempiere.c_region r ON r.c_region_id = c.c_region_id
WHERE c.postal IN ('0400', '0407')
  AND r.name = 'North West';

-- Step 3: apply the move
UPDATE adempiere.c_city c
SET c_region_id = (
        SELECT g.c_region_id
        FROM adempiere.c_region g
        WHERE g.name = 'Gauteng'
          AND g.c_country_id = (
              SELECT nw.c_country_id
              FROM adempiere.c_region nw
              WHERE nw.c_region_id = c.c_region_id
          )
    )
WHERE c.postal IN ('0400', '0407')
  AND c.c_region_id IN (
        SELECT c_region_id FROM adempiere.c_region WHERE name = 'North West'
  );

-- Step 4: verify the update landed on the expected rows
SELECT c.c_city_id, c.name, c.postal, r.name AS region
FROM adempiere.c_city c
JOIN adempiere.c_region r ON r.c_region_id = c.c_region_id
WHERE c.postal IN ('0400', '0407')
ORDER BY c.name;

-- Review the output above, then either:
COMMIT;
-- or, if anything looks wrong:
-- ROLLBACK;
