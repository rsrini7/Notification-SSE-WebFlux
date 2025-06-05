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

-- Sample users (password for all is 'password123')
INSERT INTO users (username, email, password, enabled) VALUES
('user1', 'user1@example.com', '$2a$10$GasgX6pkJ25W4LqNnsnZ4e7G0T9U0kDcXJ2kKYWzLgY2CVO5zlc/G', true),
('user2', 'user2@example.com', '$2a$10$GasgX6pkJ25W4LqNnsnZ4e7G0T9U0kDcXJ2kKYWzLgY2CVO5zlc/G', true),
('user3', 'user3@example.com', '$2a$10$GasgX6pkJ25W4LqNnsnZ4e7G0T9U0kDcXJ2kKYWzLgY2CVO5zlc/G', true);

-- It's good practice to also assign basic authorities to sample users if your system expects users to have roles.
-- For now, assuming they don't need specific roles beyond being a standard user for notification preferences to apply.
-- If they needed roles, e.g., 'USER', you would add:
-- INSERT INTO authorities (username, authority) VALUES ('user1', 'USER');
-- INSERT INTO authorities (username, authority) VALUES ('user2', 'USER');
-- INSERT INTO authorities (username, authority) VALUES ('user3', 'USER');

-- Sample user preferences
INSERT INTO user_preferences (user_id, email_enabled, sse_enabled, minimum_email_priority)
VALUES
('user1', true, true, 'HIGH'),
('user2', true, true, 'MEDIUM'),
('user3', false, true, 'CRITICAL');

-- Populate muted_notification_types based on old data.sql structure
INSERT INTO muted_notification_types (user_preferences_user_id, muted_notification_types) VALUES
('user1', 'MARKETING'),
('user1', 'SOCIAL'),
('user2', 'SYSTEM');
-- user3 had an empty list '[]', so no inserts for user3.

-- Add preferences for the admin user
INSERT INTO user_preferences (user_id, email_enabled, sse_enabled, minimum_email_priority)
VALUES
('admin', true, true, 'HIGH'); -- Schema default for minimum_email_priority is 'HIGH'
