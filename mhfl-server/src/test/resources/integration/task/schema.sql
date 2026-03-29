DROP TABLE IF EXISTS client;
DROP TABLE IF EXISTS round;
DROP TABLE IF EXISTS task;
DROP TABLE IF EXISTS algorithm;
DROP TABLE IF EXISTS dataset;
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
    create_time DATETIME     NOT NULL,
    update_time DATETIME     NOT NULL,
    delete_time DATETIME     NULL,
    is_deleted  TINYINT      NOT NULL DEFAULT 0
);

CREATE TABLE dataset
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_name   VARCHAR(255) NOT NULL,
    create_time DATETIME     NOT NULL,
    update_time DATETIME     NOT NULL,
    delete_time DATETIME     NULL,
    is_deleted  TINYINT      NOT NULL DEFAULT 0
);

CREATE TABLE algorithm
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    algorithm_name VARCHAR(255) NOT NULL,
    create_time    DATETIME     NOT NULL,
    update_time    DATETIME     NOT NULL,
    delete_time    DATETIME     NULL,
    is_deleted     TINYINT      NOT NULL DEFAULT 0
);

CREATE TABLE task
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid              BIGINT   NOT NULL,
    did              BIGINT   NOT NULL,
    aid              BIGINT   NOT NULL,
    num_nodes        INT      NOT NULL,
    fraction         DOUBLE   NOT NULL,
    classes_per_node INT      NOT NULL,
    low_prob         DOUBLE   NOT NULL,
    num_steps        INT      NOT NULL,
    epochs           INT      NOT NULL,
    loss             DOUBLE   NOT NULL DEFAULT -1,
    accuracy         DOUBLE   NOT NULL DEFAULT -1,
    `precision`      DOUBLE   NOT NULL DEFAULT -1,
    recall           DOUBLE   NOT NULL DEFAULT -1,
    f1_score         DOUBLE   NOT NULL DEFAULT -1,
    status           TINYINT  NOT NULL,
    create_time      DATETIME NOT NULL,
    update_time      DATETIME NOT NULL,
    delete_time      DATETIME NULL,
    is_deleted       TINYINT  NOT NULL DEFAULT 0
);

CREATE TABLE round
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tid         BIGINT   NOT NULL,
    round_num   INT      NOT NULL,
    loss        DOUBLE   NOT NULL DEFAULT -1,
    accuracy    DOUBLE   NOT NULL DEFAULT -1,
    `precision` DOUBLE   NOT NULL DEFAULT -1,
    recall      DOUBLE   NOT NULL DEFAULT -1,
    f1_score    DOUBLE   NOT NULL DEFAULT -1,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    delete_time DATETIME NULL,
    is_deleted  TINYINT  NOT NULL DEFAULT 0
);

CREATE TABLE client
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    rid         BIGINT   NOT NULL,
    client_index INT     NOT NULL,
    loss        DOUBLE   NOT NULL DEFAULT -1,
    accuracy    DOUBLE   NOT NULL DEFAULT -1,
    `precision` DOUBLE   NOT NULL DEFAULT -1,
    recall      DOUBLE   NOT NULL DEFAULT -1,
    f1_score    DOUBLE   NOT NULL DEFAULT -1,
    timestamp   DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    delete_time DATETIME NULL,
    is_deleted  TINYINT  NOT NULL DEFAULT 0
);
