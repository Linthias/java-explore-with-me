CREATE TABLE IF NOT EXISTS users
(
    id        BIGINT      PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    user_name VARCHAR(50) NOT NULL UNIQUE,
    email     VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS categories
(
    id            BIGINT      PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    category_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS events
(
    id                     BIGINT        PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    annotation             VARCHAR(2000) NOT NULL,
    category_id            BIGINT        NOT NULL REFERENCES categories (id),
    confirmed_requests     BIGINT        NOT NULL,
    created_on             TIMESTAMP     NOT NULL,
    description            VARCHAR(7000) NOT NULL,
    event_date             TIMESTAMP     NOT NULL,
    initiator_id           BIGINT        NOT NULL REFERENCES users (id),
    latitude               REAL          NOT NULL,
    longtitude             REAL          NOT NULL,
    paid                   BOOLEAN       NOT NULL,
    participant_limit      INTEGER       NOT NULL,
    published_on           TIMESTAMP,    -- у события может не быть даты публикации
    moderation_required    BOOLEAN       NOT NULL,
    event_state            VARCHAR(9)    NOT NULL,
    title                  VARCHAR(120)  NOT NULL,
    views                  BIGINT        NOT NULL
);

CREATE TABLE IF NOT EXISTS compilations
(
    id     BIGINT       PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    pinned BOOLEAN      NOT NULL,
    title  VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS compilations_events
(
    id             BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    compilation_id BIGINT NOT NULL UNIQUE REFERENCES compilations (id) ON DELETE CASCADE,
    event_id       BIGINT NOT NULL REFERENCES events (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS participation_requests
(
    id            BIGINT     PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    created       TIMESTAMP  NOT NULL,
    event_id      BIGINT     NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    requester_id  BIGINT     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    request_state VARCHAR(9) NOT NULL
);

CREATE TABLE IF NOT EXISTS users_followers
(
    id          BIGINT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    follower_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE
);