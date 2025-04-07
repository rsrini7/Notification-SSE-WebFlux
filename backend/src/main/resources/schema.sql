-- Drop table if exists to avoid conflicts
DROP TABLE IF EXISTS notifications;

-- Create notifications table
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    source_service VARCHAR(255) NOT NULL,
    notification_type VARCHAR(255) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    metadata VARCHAR(4000),
    tags VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    read_status VARCHAR(50) NOT NULL
);

-- Create indexes for better query performance
CREATE INDEX idx_user_id ON notifications(user_id);
CREATE INDEX idx_notification_type ON notifications(notification_type);
CREATE INDEX idx_priority ON notifications(priority);
CREATE INDEX idx_created_at ON notifications(created_at);
CREATE INDEX idx_read_status ON notifications(read_status);