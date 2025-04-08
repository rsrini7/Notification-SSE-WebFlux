#!/bin/bash

# Backend Shutdown Script
# This script stops the backend components of the notification system:
# - Backend Spring Boot application
# - Docker services (Kafka, Zookeeper, MailCrab)

echo "===== Stopping Backend Services ====="

# 1. Stop Backend process
if [ -f .backend_pid ]; then
  read BACKEND_PID < .backend_pid
  
  # Stop Backend
  if [ ! -z "$BACKEND_PID" ]; then
    echo "Stopping Backend (PID: $BACKEND_PID)..."
    kill -9 $BACKEND_PID 2>/dev/null || true
  fi
  
  # Remove PID file
  rm .backend_pid
else
  echo "No backend PID file found. Attempting to find and stop process..."
  
  # Try to find and kill Spring Boot process
  pkill -f "spring-boot:run" || true
fi

# Force kill any processes using the backend port
echo "Ensuring port 8080 is free..."
lsof -ti:8080 | xargs kill -9 2>/dev/null || true

# 2. Stop Docker Compose services
echo "Stopping Docker Compose services..."
docker-compose down

echo "===== Backend Services Stopped ====="