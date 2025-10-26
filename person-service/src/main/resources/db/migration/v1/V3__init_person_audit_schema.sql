CREATE TABLE person_aud.revinfo
(
    rev       BIGSERIAL PRIMARY KEY,
    revtmstmp BIGINT
);

CREATE TABLE person_aud.countries_aud
(
    id            INTEGER                     NOT NULL,
    rev      BIGINT                      NOT NULL,
    revtype SMALLINT                    NOT NULL,
    active        BOOLEAN                     NOT NULL DEFAULT TRUE,
    created       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    updated       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    name          VARCHAR(128)                NOT NULL,
    code          VARCHAR(3)                  NOT NULL,

    CONSTRAINT pk_counties_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_countries_aud_rev FOREIGN KEY (rev) REFERENCES person_aud.revinfo (rev)
);

CREATE INDEX IF NOT EXISTS idx_countries_aud_rev ON person_aud.countries_aud (rev);

CREATE TABLE person_aud.addresses_aud
(
    id            UUID                        NOT NULL,
    rev      BIGINT                      NOT NULL,
    revtype SMALLINT                    NOT NULL,
    active        BOOLEAN                     NOT NULL DEFAULT TRUE,
    created       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    updated       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    country_id    INTEGER                     NOT NULL,
    address       VARCHAR(128)                NOT NULL,
    zip_code      VARCHAR(32)                 NOT NULL,
    city          VARCHAR(128)                NOT NULL,

    CONSTRAINT pk_addresses_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_addresses_aud_rev FOREIGN KEY (rev) REFERENCES person_aud.revinfo (rev)
);

CREATE INDEX IF NOT EXISTS idx_addresses_aud_rev ON person_aud.addresses_aud (rev);


CREATE TABLE person_aud.users_aud
(
    id            UUID                        NOT NULL,
    rev      BIGINT                      NOT NULL,
    revtype SMALLINT                    NOT NULL,
    active        BOOLEAN                     NOT NULL DEFAULT TRUE,
    created       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    updated       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    email         VARCHAR(1024)               NOT NULL,
    first_name    VARCHAR(64)                 NOT NULL,
    last_name     VARCHAR(64)                 NOT NULL,
    address_id    UUID                        NOT NULL,

    CONSTRAINT pk_users_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_users_aud_rev FOREIGN KEY (rev) REFERENCES person_aud.revinfo (rev)
);

CREATE INDEX IF NOT EXISTS idx_users_aud_rev ON person_aud.users_aud (rev);

CREATE TABLE person_aud.individuals_aud
(
    id              UUID                        NOT NULL,
    rev        BIGINT                      NOT NULL,
    revtype   SMALLINT                    NOT NULL,
    active          BOOLEAN                     NOT NULL DEFAULT TRUE,
    created         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    updated         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    passport_number VARCHAR(64)                 NOT NULL,
    phone_number    VARCHAR(64)                 NOT NULL,
    user_id         UUID                        NOT NULL,

    CONSTRAINT pk_individuals_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_individuals_aud_rev FOREIGN KEY (rev) REFERENCES person_aud.revinfo (rev)
);

CREATE INDEX IF NOT EXISTS idx_individuals_aud_rev ON person_aud.individuals_aud (rev);