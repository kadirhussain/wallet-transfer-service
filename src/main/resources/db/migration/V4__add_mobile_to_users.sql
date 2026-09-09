ALTER TABLE users
    ADD COLUMN mobile VARCHAR(20) NOT NULL,
    ADD CONSTRAINT uq_users_mobile UNIQUE (mobile);