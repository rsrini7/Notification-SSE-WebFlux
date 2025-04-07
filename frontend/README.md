# Notification System Frontend

This directory contains two separate frontend applications for the notification system:

## 1. User UI

A React application for end users to:
- Log in and authenticate
- View their notifications in real-time
- Mark notifications as read
- Filter and search notifications

## 2. Admin UI

A React application for administrators to:
- Log in with admin privileges
- Send manual notifications to specific users or broadcast to all users
- Send critical notifications
- Monitor notification delivery status

## Development

Each application is a separate React application with its own package.json and dependencies.

### Starting the User UI
```
cd user-ui
npm install
npm start
```

### Starting the Admin UI
```
cd admin-ui
npm install
npm start
```

## Architecture

### User UI
- Uses WebSocket for real-time notification updates
- JWT authentication for secure API access
- React Router for navigation
- Material UI for components

### Admin UI
- REST API for sending notifications
- JWT authentication with admin role
- Form validation for notification creation
- Material UI for components

## Backend Integration

Both UIs connect to the notification service backend running on port 8080.