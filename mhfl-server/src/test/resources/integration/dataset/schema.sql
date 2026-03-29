DROP TABLE IF EXISTS dataset;

CREATE TABLE dataset
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_name   VARCHAR(255) NOT NULL,
    create_time DATETIME     NOT NULL,
    update_time DATETIME     NOT NULL,
    delete_time DATETIME     NULL,
    is_deleted  TINYINT      NOT NULL DEFAULT 0
);
