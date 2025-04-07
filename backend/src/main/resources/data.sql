-- Sample data for notifications table
INSERT INTO notifications (user_id, source_service, notification_type, priority, content, metadata, tags, created_at, read_status)
VALUES
-- User 1 notifications
('user1', 'email-service', 'ACCOUNT', 'HIGH', 'Your account password was changed', '{"ip":"192.168.1.1","browser":"Chrome"}', '["security","account"]', CURRENT_TIMESTAMP() - 1, 'READ'),
('user1', 'payment-service', 'PAYMENT', 'CRITICAL', 'Payment of $99.99 was processed', '{"paymentId":"PAY123456","method":"credit_card"}', '["payment","billing"]', CURRENT_TIMESTAMP() - 2, 'READ'),
('user1', 'order-service', 'ORDER', 'MEDIUM', 'Your order #12345 has been shipped', '{"orderId":"ORD12345","trackingNumber":"TRK789012"}', '["order","shipping"]', CURRENT_TIMESTAMP() - 3, 'UNREAD'),
('user1', 'system', 'SYSTEM', 'LOW', 'System maintenance scheduled for tomorrow', '{"startTime":"2023-06-15T22:00:00","endTime":"2023-06-16T02:00:00"}', '["system","maintenance"]', CURRENT_TIMESTAMP() - 4, 'UNREAD'),

-- User 2 notifications
('user2', 'email-service', 'ACCOUNT', 'HIGH', 'New login detected from unknown device', '{"ip":"203.0.113.42","location":"New York"}', '["security","login"]', CURRENT_TIMESTAMP() - 1, 'READ'),
('user2', 'payment-service', 'PAYMENT', 'MEDIUM', 'Your subscription will renew in 3 days', '{"subscriptionId":"SUB789","amount":"19.99"}', '["subscription","payment"]', CURRENT_TIMESTAMP() - 2, 'UNREAD'),
('user2', 'social-service', 'SOCIAL', 'LOW', 'User3 mentioned you in a comment', '{"postId":"POST456","commentId":"CMT123"}', '["social","mention"]', CURRENT_TIMESTAMP() - 5, 'UNREAD'),

-- User 3 notifications
('user3', 'system', 'SECURITY', 'CRITICAL', 'Suspicious login attempt blocked', '{"attempts":3,"ip":"198.51.100.23"}', '["security","login"]', CURRENT_TIMESTAMP() - 1, 'READ'),
('user3', 'order-service', 'ORDER', 'HIGH', 'Your order #54321 has been delivered', '{"orderId":"ORD54321","deliveryTime":"2023-06-14T14:30:00"}', '["order","delivery"]', CURRENT_TIMESTAMP() - 3, 'READ'),
('user3', 'marketing-service', 'MARKETING', 'LOW', 'New products available in your area', '{"category":"electronics","discount":"15%"}', '["marketing","promotion"]', CURRENT_TIMESTAMP() - 7, 'UNREAD');

-- Admin user with bcrypt password 'admin123'
INSERT INTO users (username, password, enabled) VALUES
('admin', '$2a$10$Dow1k5X5X0h5k5X5X0h5kOQ0h5k5X5X0h5k5X5X0h5k5X5X0h5k5K', true);

INSERT INTO authorities (username, authority) VALUES
('admin', 'ROLE_ADMIN');

-- Sample user preferences
INSERT INTO user_preferences (user_id, email_enabled, websocket_enabled, minimum_email_priority, muted_notification_types)
VALUES
('user1', true, true, 'HIGH', '["MARKETING", "SOCIAL"]'),
('user2', true, true, 'MEDIUM', '["SYSTEM"]'),
('user3', false, true, 'CRITICAL', '[]');
