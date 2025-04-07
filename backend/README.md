# Notification Service

A microservice for managing and delivering notifications through multiple channels (REST API, WebSocket, Kafka, Email).

## Architecture

### End-to-End Flow

1. **Notification Creation**
   - External services send notification events via REST API or Kafka
   - Notifications can be sent to specific users or broadcast to all users
   - Critical notifications can be flagged for additional delivery methods (email)

2. **Notification Processing**
   - `NotificationProcessorService` validates and processes notifications
   - Notifications are persisted in the database
   - Notifications are dispatched through appropriate channels

3. **Notification Delivery**
   - WebSocket for real-time delivery to connected users
   - Email for critical notifications
   - REST API for clients to fetch notifications

4. **Notification Management**
   - Users can mark notifications as read
   - Users can search and filter notifications
   - Users can view notification history

## Package Structure

```
com.example.notification
├── config/                  # Configuration classes
│   ├── KafkaConfig.java     # Kafka configuration
│   ├── SecurityConfig.java  # Security configuration
│   └── WebSocketConfig.java # WebSocket configuration
├── controller/              # REST API controllers
│   └── NotificationController.java # Notification REST endpoints
├── dto/                     # Data Transfer Objects
│   ├── NotificationEvent.java    # Incoming notification events
│   └── NotificationResponse.java # Outgoing notification responses
├── kafka/                   # Kafka consumers
│   ├── BroadcastNotificationConsumer.java # For broadcast notifications
│   ├── CriticalNotificationConsumer.java  # For critical notifications
│   └── NotificationConsumer.java          # For standard notifications
├── model/                   # Domain models
│   ├── Notification.java         # Notification entity
│   ├── NotificationPriority.java # Notification priority enum
│   └── NotificationStatus.java   # Notification status enum
├── repository/              # Data access layer
│   └── NotificationRepository.java # Repository for notifications
├── security/                # Security components
│   ├── JwtAuthenticationFilter.java # JWT authentication filter
│   └── JwtTokenProvider.java        # JWT token provider
├── service/                 # Business logic
│   ├── EmailService.java              # Email delivery service
│   ├── NotificationProcessorService.java # Core notification processing
│   ├── NotificationService.java         # Notification management
│   └── UserService.java                 # User-related operations
└── websocket/               # WebSocket components
    ├── WebSocketEventListener.java   # WebSocket event listener
    └── WebSocketSessionManager.java  # WebSocket session management
```

## API Endpoints

### Notification Management

- `GET /api/notifications/user/{userId}` - Get all notifications for a user
- `GET /api/notifications/user/{userId}/unread` - Get unread notifications for a user
- `GET /api/notifications/user/{userId}/type/{notificationType}` - Get notifications by type
- `GET /api/notifications/user/{userId}/search?searchTerm=term` - Search notifications
- `GET /api/notifications/{id}` - Get a notification by ID
- `PUT /api/notifications/{id}/read?userId=userId` - Mark a notification as read
- `PUT /api/notifications/user/{userId}/read-all` - Mark all notifications as read
- `GET /api/notifications/user/{userId}/unread/count` - Count unread notifications

### Notification Sending

- `POST /api/notifications` - Send a notification to specific users
- `POST /api/notifications/broadcast` - Send a broadcast notification
- `POST /api/notifications/critical` - Send a critical notification

## WebSocket Topics

- `/user/{userId}/notifications` - User-specific notifications
- `/topic/broadcast` - Broadcast notifications

## Kafka Topics

- `notifications` - Standard notifications
- `broadcast-notifications` - Broadcast notifications
- `critical-notifications` - Critical notifications