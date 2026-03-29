DROP TABLE IF EXISTS account;

CREATE TABLE account
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    gender      TINYINT      NOT NULL,
    email       VARCHAR(64)  NOT NULL,
    telephone   VARCHAR(20)  NOT NULL,
    avatar      VARCHAR(255) NULL,
    role        VARCHAR(20)  NOT NULL,
    birthday    DATE         NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delete_time DATETIME     NULL,
    is_deleted  TINYINT      NOT NULL DEFAULT 0
);
