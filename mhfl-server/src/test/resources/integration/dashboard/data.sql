INSERT INTO account (id, username, password, gender, email, telephone, avatar, role, birthday, create_time, update_time, delete_time, is_deleted)
VALUES (1, 'admin', 'pwd', 1, 'admin@example.com', '13800000000', NULL, 'admin', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (2, 'user1', 'pwd', 1, 'user1@example.com', '13900000001', NULL, 'user', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);

INSERT INTO dataset (id, data_name, create_time, update_time, delete_time, is_deleted)
VALUES (1, 'CIFAR-100', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (2, 'Tiny-ImageNet', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);

INSERT INTO algorithm (id, algorithm_name, create_time, update_time, delete_time, is_deleted)
VALUES (1, 'FedAvg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (2, 'FedProto', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);

INSERT INTO task (id, uid, did, aid, num_nodes, fraction, classes_per_node, low_prob, num_steps, epochs, loss, accuracy, `precision`, recall, f1_score, status, create_time, update_time, delete_time, is_deleted)
VALUES (1, 2, 1, 1, 100, 0.1, 2, 0.2, 500, 10, 0.5, 0.7, 0.71, 0.72, 0.73, 0, DATEADD('DAY', -1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, NULL, 0),
       (2, 2, 1, 1, 100, 0.1, 2, 0.2, 500, 10, 0.4, 0.8, 0.81, 0.82, 0.83, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (3, 2, 2, 2, 100, 0.1, 2, 0.2, 500, 10, 0.3, 0.85, 0.86, 0.87, 0.88, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (4, 1, 2, 2, 100, 0.1, 2, 0.2, 500, 10, 0.3, 0.86, 0.87, 0.88, 0.89, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (5, 2, 1, 2, 100, 0.1, 2, 0.2, 500, 10, 0.9, 0.5, 0.51, 0.52, 0.53, 4, DATEADD('DAY', -2, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, NULL, 0);
