-- Sample data for notifications table
INSERT INTO notifications (user_id, source_service, notification_type, title, priority, content, created_at, read_status)
VALUES
-- User 1 notifications
('user1', 'email-service', 'ACCOUNT', 'Password Changed', 'HIGH', 'Your account password was changed', CURRENT_TIMESTAMP() - 1, 'READ'),
('user1', 'payment-service', 'PAYMENT', 'Payment Processed', 'CRITICAL', 'Payment of $99.99 was processed', CURRENT_TIMESTAMP() - 2, 'READ'),
('user1', 'order-service', 'ORDER', 'Order Shipped', 'MEDIUM', 'Your order #12345 has been shipped', CURRENT_TIMESTAMP() - 3, 'UNREAD'),
('user1', 'system', 'SYSTEM', 'Maintenance Scheduled', 'LOW', 'System maintenance scheduled for tomorrow', CURRENT_TIMESTAMP() - 4, 'UNREAD'),

-- User 2 notifications
('user2', 'email-service', 'ACCOUNT', 'New Login Detected', 'HIGH', 'New login detected from unknown device', CURRENT_TIMESTAMP() - 1, 'READ'),
('user2', 'payment-service', 'PAYMENT', 'Subscription Renewal', 'MEDIUM', 'Your subscription will renew in 3 days', CURRENT_TIMESTAMP() - 2, 'UNREAD'),
('user2', 'social-service', 'SOCIAL', 'Mentioned in Comment', 'LOW', 'User3 mentioned you in a comment', CURRENT_TIMESTAMP() - 5, 'UNREAD'),

-- User 3 notifications
('user3', 'system', 'SECURITY', 'Suspicious Login Blocked', 'CRITICAL', 'Suspicious login attempt blocked', CURRENT_TIMESTAMP() - 1, 'READ'),
('user3', 'order-service', 'ORDER', 'Order Delivered', 'HIGH', 'Your order #54321 has been delivered', CURRENT_TIMESTAMP() - 3, 'READ'),
('user3', 'marketing-service', 'MARKETING', 'New Products Available', 'LOW', 'New products available in your area', CURRENT_TIMESTAMP() - 7, 'UNREAD');

-- Admin user with bcrypt password 'admin123'
INSERT INTO users (username, password, enabled) VALUES
('admin', '$2b$12$NWFZFsRil3BmZLpGGnuCROubOmJ6lKiqrsLP3yVb3LkdJwJje9duq', true);

INSERT INTO authorities (username, authority) VALUES
('admin', 'ROLE_ADMIN');

-- Sample user preferences
INSERT INTO user_preferences (user_id, email_enabled, websocket_enabled, minimum_email_priority, muted_notification_types)
VALUES
('user1', true, true, 'HIGH', '["MARKETING", "SOCIAL"]'),
('user2', true, true, 'MEDIUM', '["SYSTEM"]'),
('user3', false, true, 'CRITICAL', '[]');
