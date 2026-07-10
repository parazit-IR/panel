-- Hibernate generates UUID identifiers in the application using @UuidGenerator.
-- PostgreSQL extensions such as uuid-ossp or pgcrypto are therefore not needed
-- for identifier generation at this stage.
--
-- This migration intentionally establishes the initial Flyway baseline without
-- creating business tables.
SELECT 1;
