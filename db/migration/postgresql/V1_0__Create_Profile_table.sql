-- Table: Profile (User + Author)
-- ~~~~~~
CREATE TABLE IF NOT EXISTS profile(
    username   VARCHAR   NOT NULL,
    email      VARCHAR   NOT NULL,
    bio        VARCHAR,
    image      VARCHAR,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_profile_username       PRIMARY KEY (username),
    CONSTRAINT uq_profile_email    UNIQUE      (email)
);

CREATE INDEX IF NOT EXISTS ix_profile_email
    ON profile
    USING btree (lower(email));
