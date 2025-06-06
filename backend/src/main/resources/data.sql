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

-- Admin user with bcrypt password 'admin123'
INSERT INTO users (username, email, password, enabled) VALUES
('admin', 'admin@example.com', '$2b$12$NWFZFsRil3BmZLpGGnuCROubOmJ6lKiqrsLP3yVb3LkdJwJje9duq', true);

-- Sample users (password for all is 'password123')
INSERT INTO users (username, email, password, enabled) VALUES
('user1', 'user1@example.com', '$2b$12$NWFZFsRil3BmZLpGGnuCROubOmJ6lKiqrsLP3yVb3LkdJwJje9duq', true),
('user2', 'user2@example.com', '$2b$12$NWFZFsRil3BmZLpGGnuCROubOmJ6lKiqrsLP3yVb3LkdJwJje9duq', true),
('user3', 'user3@example.com', '$2b$12$NWFZFsRil3BmZLpGGnuCROubOmJ6lKiqrsLP3yVb3LkdJwJje9duq', true),
('user4', 'user4@example.com', '$2b$12$NWFZFsRil3BmZLpGGnuCROubOmJ6lKiqrsLP3yVb3LkdJwJje9duq', true);

-- It's good practice to also assign basic authorities to sample users if your system expects users to have roles.
-- For now, assuming they don't need specific roles beyond being a standard user for notification preferences to apply.
-- If they needed roles, e.g., 'USER', you would add:
INSERT INTO authorities (username, authority) VALUES ('user1', 'USER');
INSERT INTO authorities (username, authority) VALUES ('user2', 'USER');
INSERT INTO authorities (username, authority) VALUES ('user3', 'USER');
INSERT INTO authorities (username, authority) VALUES ('user4', 'USER');

INSERT INTO authorities (username, authority) VALUES ('admin', 'ADMIN');

-- Sample user preferences
INSERT INTO user_preferences (user_id, email_enabled, sse_enabled, minimum_email_priority)
VALUES
('user1', true, true, 'HIGH'),
('user2', true, false, 'MEDIUM'),
('user3', false, true, 'CRITICAL'),
('user4', false, false, 'CRITICAL');

INSERT INTO user_preferences (user_id, email_enabled, sse_enabled, minimum_email_priority)
VALUES
('admin', true, true, 'CRITICAL'); -- Schema default for minimum_email_priority is 'CRITICAL'

-- Populate muted_notification_types based on old data.sql structure
INSERT INTO muted_notification_types (user_preferences_user_id, muted_notification_types) VALUES
('user1', 'MARKETING'),
('user1', 'SOCIAL'),
('user2', 'SYSTEM');
-- user3 had an empty list '[]', so no inserts for user3.

