CREATE SCHEMA IF NOT EXISTS person;
CREATE SCHEMA IF NOT EXISTS person_aud;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
SET search_path TO person,person_aud,public;