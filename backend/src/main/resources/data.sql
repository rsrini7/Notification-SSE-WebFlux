-- Insert notification types
INSERT INTO notification_types (type_code, description) VALUES
('ACCOUNT', 'Account related notifications'),
('PAYMENT', 'Payment and billing related notifications'),
('ORDER', 'Order status and shipping notifications'),
('SYSTEM', 'System and maintenance notifications'),
('SECURITY', 'Security related notifications'),
('SOCIAL', 'Social interactions and mentions'),
('MARKETING', 'Marketing and promotional notifications');

-- Sample data for notifications table
INSERT INTO notifications (user_id, source_service, notification_type_id, title, priority, content, created_at, read_status)
SELECT 
    'user1', 
    'email-service', 
    (SELECT id FROM notification_types WHERE type_code = 'ACCOUNT'), 
    'Password Changed', 
    'HIGH', 
    'Your account password was changed', 
    CURRENT_TIMESTAMP() - INTERVAL '1' DAY, 
    'READ';

-- INSERT INTO notifications (user_id, source_service, notification_type_id, title, priority, content, created_at, read_status)
-- SELECT 
--     'user1',
--     'payment-service',
--     (SELECT id FROM notification_types WHERE type_code = 'PAYMENT'),
--     'Payment Processed',
--     'CRITICAL',
--     'Payment of $99.99 was processed',
--     CURRENT_TIMESTAMP() - INTERVAL '2' DAY,
--     'READ';
-- 
-- INSERT INTO notifications (user_id, source_service, notification_type_id, title, priority, content, created_at, read_status)
-- SELECT 
--     'user1',
--     'order-service',
--     (SELECT id FROM notification_types WHERE type_code = 'ORDER'),
--     'Order Shipped',
--     'MEDIUM',
--     'Your order #12345 has been shipped',
--     CURRENT_TIMESTAMP() - INTERVAL '3' DAY,
--     'UNREAD';

-- Admin user with bcrypt password 'admin123'
INSERT INTO users (username, email, password, enabled) VALUES
('admin', 'admin@example.com', '$2b$12$NWFZFsRil3BmZLpGGnuCROubOmJ6lKiqrsLP3yVb3LkdJwJje9duq', true);

INSERT INTO authorities (username, authority) VALUES
('admin', 'ADMIN');

-- Sample user preferences
INSERT INTO user_preferences (user_id, email_enabled, websocket_enabled, minimum_email_priority, muted_notification_types)
VALUES
('user1', true, true, 'HIGH', '["MARKETING", "SOCIAL"]'),
('user2', true, true, 'MEDIUM', '["SYSTEM"]'),
('user3', false, true, 'CRITICAL', '[]');
