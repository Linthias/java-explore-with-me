CREATE TABLE IF NOT EXISTS endpoint_hits
(
    id            BIGINT        NOT NULL GENERATED BY DEFAULT AS IDENTITY,
    app           VARCHAR(100)  NOT NULL,
    uri           VARCHAR(2000) NOT NULL,
    ip            VARCHAR(40)   NOT NULL,
    hit_timestamp TIMESTAMP     NOT NULL,
    CONSTRAINT pk_hit PRIMARY KEY (id)
);