INSERT INTO account (id, username, password, gender, email, telephone, avatar, role, birthday, create_time, update_time, delete_time, is_deleted)
VALUES (1, 'admin', '$2a$10$abcdefghijklmnopqrstuv', 1, 'admin@example.com', '13800000000', NULL, 'admin', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (2, 'user1', '$2a$10$abcdefghijklmnopqrstuv', 1, 'user1@example.com', '13900000001', NULL, 'user', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (3, 'user2', '$2a$10$abcdefghijklmnopqrstuv', 2, 'user2@example.com', '13900000002', NULL, 'user', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);
