-- Uniqueness for wilayah data is enforced at the service layer (WilayahService).
-- DB-level UNIQUE constraints were not applied because the existing seeded data
-- contains historical entries (e.g., Maluku Utara with two province IDs) that
-- would violate a strict constraint. The service layer prevents new duplicates.
SELECT 1;
