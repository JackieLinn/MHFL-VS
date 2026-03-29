DROP TABLE IF EXISTS message;
DROP TABLE IF EXISTS conversation;
DROP TABLE IF EXISTS account;

CREATE TABLE account
(
    id          BIGINT PRIMARY KEY,
    username    VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    gender      TINYINT      NOT NULL,
    email       VARCHAR(64)  NOT NULL,
    telephone   VARCHAR(20)  NOT NULL,
    avatar      VARCHAR(255) NULL,
    role        VARCHAR(20)  NOT NULL,
    birthday    DATE         NULL,
    create_time DATETIME     NOT NULL,
    update_time DATETIME     NOT NULL,
    delete_time DATETIME     NULL,
    is_deleted  TINYINT      NOT NULL DEFAULT 0
);

CREATE TABLE conversation
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    uid           BIGINT       NOT NULL,
    title         VARCHAR(255) NOT NULL,
    summary       TEXT         NULL,
    message_count INT          NOT NULL DEFAULT 0,
    create_time   DATETIME     NOT NULL,
    update_time   DATETIME     NOT NULL,
    delete_time   DATETIME     NULL,
    is_deleted    TINYINT      NOT NULL DEFAULT 0
);

CREATE TABLE message
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    cid          BIGINT       NOT NULL,
    role         VARCHAR(20)  NOT NULL,
    content      LONGTEXT     NOT NULL,
    sequence_num INT          NOT NULL,
    sources_json JSON         NULL,
    feedback     TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     NOT NULL,
    update_time  DATETIME     NOT NULL,
    delete_time  DATETIME     NULL,
    is_deleted   TINYINT      NOT NULL DEFAULT 0
);
