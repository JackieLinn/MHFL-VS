DROP TABLE IF EXISTS algorithm;

CREATE TABLE algorithm
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    algorithm_name VARCHAR(255) NOT NULL,
    create_time    DATETIME     NOT NULL,
    update_time    DATETIME     NOT NULL,
    delete_time    DATETIME     NULL,
    is_deleted     TINYINT      NOT NULL DEFAULT 0
);
