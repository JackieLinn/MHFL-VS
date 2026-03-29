INSERT INTO account (id, username, password, gender, email, telephone, avatar, role, birthday, create_time, update_time, delete_time, is_deleted)
VALUES (1, 'admin', 'pwd', 1, 'admin@example.com', '13800000000', NULL, 'admin', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (2, 'user1', 'pwd', 1, 'user1@example.com', '13900000001', NULL, 'user', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (3, 'user2', 'pwd', 1, 'user2@example.com', '13900000002', NULL, 'user', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);

INSERT INTO dataset (id, data_name, create_time, update_time, delete_time, is_deleted)
VALUES (1, 'CIFAR-100', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);

INSERT INTO algorithm (id, algorithm_name, create_time, update_time, delete_time, is_deleted)
VALUES (1, 'FedAvg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);

INSERT INTO task (id, uid, did, aid, num_nodes, fraction, classes_per_node, low_prob, num_steps, epochs, loss, accuracy, `precision`, recall, f1_score, status, create_time, update_time, delete_time, is_deleted)
VALUES (1, 2, 1, 1, 5, 0.2, 2, 0.1, 50, 3, -1, -1, -1, -1, -1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (2, 2, 1, 1, 5, 0.2, 2, 0.1, 50, 3, 0.5, 0.7, 0.71, 0.72, 0.73, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (3, 2, 1, 1, 3, 0.5, 2, 0.1, 10, 2, 0.4, 0.8, 0.81, 0.82, 0.83, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (4, 2, 1, 1, 3, 0.5, 2, 0.1, 10, 2, 0.35, 0.85, 0.86, 0.87, 0.88, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (5, 3, 1, 1, 3, 0.5, 2, 0.1, 10, 2, 0.45, 0.75, 0.76, 0.77, 0.78, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);

INSERT INTO round (id, tid, round_num, loss, accuracy, `precision`, recall, f1_score, create_time, update_time, delete_time, is_deleted)
VALUES (101, 3, 0, 0.9, 0.55, 0.56, 0.57, 0.58, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (102, 3, 1, 0.7, 0.68, 0.69, 0.70, 0.71, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (103, 3, 2, 0.5, 0.80, 0.81, 0.82, 0.83, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);

INSERT INTO client (id, rid, client_index, loss, accuracy, `precision`, recall, f1_score, timestamp, create_time, update_time, delete_time, is_deleted)
VALUES (201, 101, 0, 0.95, 0.50, 0.51, 0.52, 0.53, DATEADD('MINUTE', -3, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (202, 102, 0, 0.75, 0.65, 0.66, 0.67, 0.68, DATEADD('MINUTE', -2, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (203, 103, 0, 0.55, 0.85, 0.86, 0.87, 0.88, DATEADD('MINUTE', -1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (204, 102, 1, 0.72, 0.67, 0.68, 0.69, 0.70, DATEADD('MINUTE', -2, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);
