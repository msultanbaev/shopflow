CREATE TABLE users.users (
                                    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    keycloak_id VARCHAR(255) NOT NULL UNIQUE,
                                    email       VARCHAR(255) NOT NULL UNIQUE,
                                    first_name  VARCHAR(100) NOT NULL,
                                    last_name   VARCHAR(100) NOT NULL,
                                    phone       VARCHAR(20),
                                    role        VARCHAR(50)  NOT NULL DEFAULT 'CUSTOMER',
                                    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
                                    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);