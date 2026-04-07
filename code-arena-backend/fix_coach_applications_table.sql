-- Run this in MySQL if the columns were not automatically created by Hibernate
-- Check first:
-- DESCRIBE coach_applications;

-- If cv_file_base64 and cv_file_name columns are missing, run:
ALTER TABLE coach_applications ADD COLUMN cv_file_base64 LONGTEXT;
ALTER TABLE coach_applications ADD COLUMN cv_file_name VARCHAR(255);
