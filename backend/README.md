# Notification Service

A microservice for managing and delivering notifications through multiple channels (REST API, Server-Sent Events (SSE), Kafka, Email).

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
   - Server-Sent Events (SSE) for real-time delivery to connected User UI clients
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
│   └── SseConfig.java       # Server-Sent Events (SSE) configuration
├── controller/              # REST API controllers
│   ├── NotificationController.java # Notification REST endpoints
│   └── SseController.java   # SSE connection endpoint
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
│   ├── SseEmitterManager.java       # Manages SSE emitters for users
│   └── UserService.java                 # User-related operations
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

## SSE Endpoints

- `GET /api/notifications/events`
  - Establishes a Server-Sent Event stream for real-time notifications.
  - **Authentication**: Requires a JWT token passed as a query parameter, e.g., `/api/notifications/events?token=<JWT_TOKEN_HERE>`.
  - The User UI connects to this endpoint using the `EventSource` API.
  - User-specific notifications are pushed to the respective user's stream. Broadcast notifications might be handled by sending to all active user streams or by client-side filtering if a common stream is used (currently, it's per-user).

## Kafka Topics

- `notifications` - Standard notifications
- `broadcast-notifications` - Broadcast notifications
- `critical-notifications` - Critical notifications