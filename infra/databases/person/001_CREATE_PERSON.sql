DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_databases WHERE datname = 'person') THEN
        CREATE DATABASE person;
    END IF;
END $$;