-- Table: Authentication
-- ~~~~~~
CREATE TABLE IF NOT EXISTS comment(
    id          BIGSERIAL NOT NULL,
    article_id  BIGINT    NOT NULL,
    body        VARCHAR NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    author_username      VARCHAR    NOT NULL,
    CONSTRAINT pk_comment_id      PRIMARY KEY (id),
    CONSTRAINT fk_comment_profile FOREIGN KEY (author_username) REFERENCES profile (username),
    CONSTRAINT fk_comment_article FOREIGN KEY (article_id) REFERENCES article (id)
);

CREATE INDEX IF NOT EXISTS ix_article_slag
    ON article
    USING btree (lower(slag));