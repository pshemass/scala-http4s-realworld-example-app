-- Table: Authentication
-- ~~~~~~
CREATE TABLE IF NOT EXISTS article(
    id          BIGSERIAL NOT NULL,
    slag        VARCHAR NOT NULL,
    title       VARCHAR NOT NULL,
    description VARCHAR NOT NULL,
    body        VARCHAR NOT NULL,
    tags        varchar[],
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    author_username      VARCHAR    NOT NULL,
    CONSTRAINT pk_article_id      PRIMARY KEY (id),
    CONSTRAINT fk_article_profile FOREIGN KEY (author_username) REFERENCES profile (username)
);

CREATE INDEX IF NOT EXISTS ix_article_slag_username
    ON article
    USING btree (lower(slag));