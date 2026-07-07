-- 时间精度更新 SQL（精确到 HH:mm）
-- 执行方式：mysql -uenroll -p***REMOVED*** enroll_db < docs/sql/update_timespec_format.sql

UPDATE ssc_classes SET
  period = '2026/09/01 08:00 - 2026/09/13 23:59',
  periods = '[{"round":1,"period":"2026/09/01 08:00 - 2026/09/13 23:59"}]'
WHERE id = 1;

UPDATE ssc_classes SET
  period = '2026/09/01 08:00 - 2026/09/13 23:59',
  periods = '[{"round":1,"period":"2026/09/01 08:00 - 2026/09/13 23:59"},{"round":2,"period":"2026/09/15 08:00 - 2026/09/16 23:59"}]'
WHERE id IN (2, 3);

UPDATE ssc_classes SET
  period = '2026/08/15 08:00 - 2026/09/16 23:59',
  periods = '[{"round":1,"period":"2026/08/15 08:00 - 2026/09/16 23:59"}]'
WHERE id IN (4, 5);

UPDATE ssc_classes SET
  period = '2026/08/15 08:00 - 2026/08/25 23:59',
  periods = '[{"round":1,"period":"2026/08/15 08:00 - 2026/08/25 23:59"}]'
WHERE id = 6;

UPDATE ssc_classes SET
  period = '2026/08/05 08:00 - 2026/08/15 23:59',
  periods = '[{"round":1,"period":"2026/08/05 08:00 - 2026/08/15 23:59"}]'
WHERE id = 7;
