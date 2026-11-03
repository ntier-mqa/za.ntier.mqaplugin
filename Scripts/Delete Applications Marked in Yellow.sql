-- BACKUP TABLES (dated 29-10-2026)
CREATE TABLE t_zz_application_form29102026 AS
SELECT * FROM zz_application_form;

-- RELATED TABLES
CREATE TABLE t_zz_formcontact29102026 AS
SELECT * FROM zz_formcontact;

CREATE TABLE t_zz_formdiscipline29102026 AS
SELECT * FROM zz_formdiscipline;

CREATE TABLE t_zzannexure29102026 AS
SELECT * FROM zzannexure;

CREATE TABLE t_zzsubannex29102026 AS
SELECT * FROM zzsubannex;

CREATE TABLE t_zzdocumentuploadfile29102026 AS
SELECT * FROM zzdocumentuploadfile;

CREATE TABLE t_zzlearnersapplied29102026 AS
SELECT * FROM zzlearnersapplied;

CREATE TABLE t_zzdetailsmallbusinesse29102026 AS
SELECT * FROM zzdetailsmallbusinesse;

CREATE TABLE t_zzstandardsetting29102026 AS
SELECT * FROM zzstandardsetting;

CREATE TABLE t_zzlearningmaterial29102026 AS
SELECT * FROM zzlearningmaterial;

CREATE TABLE t_zzbankingdetails29102026 AS
SELECT * FROM zzbankingdetails;


-- SANITY CHECK: confirm all 17 documentno values exist exactly once each
SELECT documentno, COUNT(*)
FROM zz_application_form
group by documentno 

WHERE documentno IN (
  '1003741','1003682','1003739','1003643','1003644','1003661','1003673',
  '1003685','1003700','1003728','1003730','1003734','1003735','1003738',
  '1003748','1003749','1003753'
)
GROUP BY documentno
ORDER BY documentno;


BEGIN;

-- Child tables (FK -> zz_application_form)

DELETE FROM zz_formcontact c
USING zz_application_form f
WHERE f.zz_application_form_id = c.zz_application_form_id
  AND f.documentno IN (
    '1003741','1003682','1003739','1003643','1003644','1003661','1003673',
    '1003685','1003700','1003728','1003730','1003734','1003735','1003738',
    '1003748','1003749','1003753'
  );

DELETE FROM zz_formdiscipline d
USING zz_application_form f
WHERE f.zz_application_form_id = d.zz_application_form_id
  AND f.documentno IN (
    '1003741','1003682','1003739','1003643','1003644','1003661','1003673',
    '1003685','1003700','1003728','1003730','1003734','1003735','1003738',
    '1003748','1003749','1003753'
  );

-- zzsubannex before zzannexure
DELETE FROM zzsubannex sa
USING zz_application_form f
WHERE f.zz_application_form_id = sa.zz_application_form_id
  AND f.documentno IN (
    '1003741','1003682','1003739','1003643','1003644','1003661','1003673',
    '1003685','1003700','1003728','1003730','1003734','1003735','1003738',
    '1003748','1003749','1003753'
  );

DELETE FROM zzannexure a
USING zz_application_form f
WHERE f.zz_application_form_id = a.zz_application_form_id
  AND f.documentno IN (
    '1003741','1003682','1003739','1003643','1003644','1003661','1003673',
    '1003685','1003700','1003728','1003730','1003734','1003735','1003738',
    '1003748','1003749','1003753'
  );

DELETE FROM zzdocumentuploadfile uf
USING zz_application_form f
WHERE f.zz_application_form_id = uf.zz_application_form_id
  AND f.documentno IN (
    '1003741','1003682','1003739','1003643','1003644','1003661','1003673',
    '1003685','1003700','1003728','1003730','1003734','1003735','1003738',
    '1003748','1003749','1003753'
  );

DELETE FROM zzlearnersapplied la
USING zz_application_form f
WHERE f.zz_application_form_id = la.zz_application_form_id
  AND f.documentno IN (
    '1003741','1003682','1003739','1003643','1003644','1003661','1003673',
    '1003685','1003700','1003728','1003730','1003734','1003735','1003738',
    '1003748','1003749','1003753'
  );

DELETE FROM zzdetailsmallbusinesse sb
USING zz_application_form f
WHERE f.zz_application_form_id = sb.zz_application_form_id
  AND f.documentno IN (
    '1003741','1003682','1003739','1003643','1003644','1003661','1003673',
    '1003685','1003700','1003728','1003730','1003734','1003735','1003738',
    '1003748','1003749','1003753'
  );

DELETE FROM zzstandardsetting ss
USING zz_application_form f
WHERE f.zz_application_form_id = ss.zz_application_form_id
  AND f.documentno IN (
    '1003741','1003682','1003739','1003643','1003644','1003661','1003673',
    '1003685','1003700','1003728','1003730','1003734','1003735','1003738',
    '1003748','1003749','1003753'
  );

DELETE FROM zzlearningmaterial lm
USING zz_application_form f
WHERE f.zz_application_form_id = lm.zz_application_form_id
  AND f.documentno IN (
    '1003741','1003682','1003739','1003643','1003644','1003661','1003673',
    '1003685','1003700','1003728','1003730','1003734','1003735','1003738',
    '1003748','1003749','1003753'
  );

DELETE FROM zzbankingdetails bd
USING zz_application_form f
WHERE f.zz_application_form_id = bd.zz_application_form_id
  AND f.documentno IN (
    '1003741','1003682','1003739','1003643','1003644','1003661','1003673',
    '1003685','1003700','1003728','1003730','1003734','1003735','1003738',
    '1003748','1003749','1003753'
  );

-- Parent table LAST
DELETE FROM zz_application_form
WHERE documentno IN (
  '1003741','1003682','1003739','1003643','1003644','1003661','1003673',
  '1003685','1003700','1003728','1003730','1003734','1003735','1003738',
  '1003748','1003749','1003753'
);

-- Review results above, then either:
-- COMMIT;
-- or
-- ROLLBACK;

