#!/bin/bash

# Frontend Shutdown Script
# This script stops the frontend components of the notification system:
# - Admin UI
# - User UI

echo "===== Stopping Frontend Services ====="

# 1. Stop Frontend processes
if [ -f .frontend_pids ]; then
  read ADMIN_UI_PID USER_UI_PID < .frontend_pids
  
  # Stop User UI
  if [ ! -z "$USER_UI_PID" ]; then
    echo "Stopping User UI (PID: $USER_UI_PID)..."
    kill -9 $USER_UI_PID 2>/dev/null || true
  fi
  
  # Stop Admin UI
  if [ ! -z "$ADMIN_UI_PID" ]; then
    echo "Stopping Admin UI (PID: $ADMIN_UI_PID)..."
    kill -9 $ADMIN_UI_PID 2>/dev/null || true
  fi
  
  # Remove PID file
  rm .frontend_pids
else
  echo "No frontend PID file found. Attempting to find and stop processes..."
  
  # Try to find and kill React processes
  pkill -f "react-scripts start" || true
fi

# Force kill any processes using the frontend ports
echo "Ensuring ports 3000 and 3001 are free..."
lsof -ti:3000 | xargs kill -9 2>/dev/null || true
lsof -ti:3001 | xargs kill -9 2>/dev/null || true

echo "===== Frontend Services Stopped ====="