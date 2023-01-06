INSERT INTO event_states (id, state_name)
VALUES (1, 'PENDING'),
       (2, 'PUBLISHED'),
       (3, 'CANCELED')
ON CONFLICT DO NOTHING;

INSERT INTO request_states (id, state_name)
VALUES (1, 'PENDING'),
       (2, 'CONFIRMED'),
       (3, 'REJECTED'),
       (4, 'CANCELED')
ON CONFLICT DO NOTHING;