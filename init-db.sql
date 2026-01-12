-- Создаем расширение для UUID если нужно
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Создаем таблицу игроков если не существует (Spring Data JPA создаст сам, но можно и так)
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'players') THEN
            CREATE TABLE players
            (
                id               BIGSERIAL PRIMARY KEY,
                username         VARCHAR(255) UNIQUE NOT NULL,
                email            VARCHAR(255),
                telegram_chat_id VARCHAR(255),
                games_played     INTEGER   DEFAULT 0,
                games_won        INTEGER   DEFAULT 0,
                created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_played_at   TIMESTAMP
            );
        END IF;
    END
$$;