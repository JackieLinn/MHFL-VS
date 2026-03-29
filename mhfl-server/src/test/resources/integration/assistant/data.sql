INSERT INTO account (id, username, password, gender, email, telephone, avatar, role, birthday, create_time, update_time, delete_time, is_deleted)
VALUES (1, 'user1', 'pwd', 1, 'user1@example.com', '13900000001', NULL, 'user', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (2, 'user2', 'pwd', 1, 'user2@example.com', '13900000002', NULL, 'user', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);

INSERT INTO conversation (id, uid, title, summary, message_count, create_time, update_time, delete_time, is_deleted)
VALUES (1, 1, '已有对话', NULL, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (2, 2, '他人对话', NULL, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (3, 1, '空对话', NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);

INSERT INTO message (id, cid, role, content, sequence_num, sources_json, feedback, create_time, update_time, delete_time, is_deleted)
VALUES (1, 1, 'user', '你好', 1, '[]', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (2, 1, 'assistant', '你好，我是助手', 2, '["doc1"]', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (3, 2, 'user', 'hello', 1, '[]', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (4, 2, 'assistant', 'world', 2, '[]', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);
