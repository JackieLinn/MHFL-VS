INSERT INTO dataset (id, data_name, create_time, update_time, delete_time, is_deleted)
VALUES (1, 'CIFAR-100', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (2, 'tiny-imagenet', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);

INSERT INTO algorithm (id, algorithm_name, create_time, update_time, delete_time, is_deleted)
VALUES (1, 'Standalone', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (2, 'FedAvg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (3, 'FedProto', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (4, 'FedSSA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (5, 'LG-FedAvg', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (6, 'FedJitter', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);

INSERT INTO task (id, uid, did, aid, num_nodes, fraction, classes_per_node, low_prob, num_steps, epochs,
                  loss, accuracy, `precision`, recall, f1_score, status, create_time, update_time, delete_time, is_deleted)
VALUES (1, 1, 1, 1, 100, 0.1, 2, 0.2, 300, 5, 0.8, 0.7, 0.6, 0.5, 0.4, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (2, 1, 1, 2, 100, 0.2, 3, 0.3, 500, 10, 0.4, 0.8, 0.81, 0.82, 0.83, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (13, 1, 1, 6, 50, 0.5, 5, 0.7, 200, 2, 0.9, 0.6, 0.61, 0.62, 0.63, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);

INSERT INTO round (id, tid, round_num, loss, accuracy, `precision`, recall, f1_score, create_time, update_time, delete_time, is_deleted)
VALUES (1001, 2, 0, 1.00, 0.70, 0.71, 0.72, 0.73, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (1002, 2, 1, 0.80, 0.77, 0.78, 0.79, 0.80, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (1301, 13, 0, 1.20, 0.60, 0.61, 0.62, 0.63, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);

INSERT INTO client (id, rid, client_index, loss, accuracy, `precision`, recall, f1_score, timestamp, create_time, update_time, delete_time, is_deleted)
VALUES (2001, 1001, 0, 1.00, 0.70, 0.71, 0.72, 0.73, '2026-03-01 10:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (2002, 1002, 0, 0.90, 0.77, 0.78, 0.79, 0.80, '2026-03-01 10:10:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (2003, 1001, 1, 1.10, 0.65, 0.66, 0.67, 0.68, '2026-03-01 10:05:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0),
       (2004, 1301, 0, 1.30, 0.60, 0.61, 0.62, 0.63, '2026-03-01 10:03:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 0);
