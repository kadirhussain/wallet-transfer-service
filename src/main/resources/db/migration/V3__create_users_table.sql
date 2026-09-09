-- V1__create_users_table.sql
-- Creates the users table and the user_roles collection table
-- backing com.wallet.transfer.domain.entity.User

CREATE TABLE users (
                       id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       name            VARCHAR(255) NOT NULL,
                       email           VARCHAR(255) NOT NULL,
                       password_hash   VARCHAR(255) NOT NULL,
                       created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

                       CONSTRAINT uq_users_name  UNIQUE (name),
                       CONSTRAINT uq_users_email UNIQUE (email)
);

-- Backs the @ElementCollection(fetch = FetchType.EAGER) Set<Role> roles
-- @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
CREATE TABLE user_roles (
                            user_id UUID NOT NULL,
                            role    VARCHAR(50) NOT NULL,

                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id) REFERENCES users (id)
                                    ON DELETE CASCADE,

                            CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),

                            CONSTRAINT ck_user_roles_role
                                CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN'))
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);