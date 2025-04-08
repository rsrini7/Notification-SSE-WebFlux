#!/bin/bash

# Frontend Startup Script
# This script starts the frontend components of the notification system:
# - Admin UI
# - User UI

echo "===== Starting Frontend Services ====="

# Function to check if a process is running on a specific port
check_port() {
  lsof -i:$1 > /dev/null 2>&1
  return $?
}

# 1. Start Admin UI
echo "Starting Admin UI..."
cd frontend/admin-ui
# Create .env file if it doesn't exist
if [ ! -f .env ]; then
  echo "PORT=3000" > .env
  echo "Created .env file for Admin UI"
fi
# Get the port from .env file
ADMIN_PORT=$(grep "PORT=" .env | cut -d '=' -f2)

# Check if port is already in use
if check_port $ADMIN_PORT; then
  echo "Warning: Port $ADMIN_PORT is already in use!"
  read -p "Would you like to use a different port? (y/n): " change_port
  if [ "$change_port" = "y" ]; then
    read -p "Enter new port number: " new_port
    sed -i '' "s/PORT=$ADMIN_PORT/PORT=$new_port/" .env
    ADMIN_PORT=$new_port
    echo "Updated Admin UI port to $ADMIN_PORT"
  else
    echo "Attempting to kill the process using port $ADMIN_PORT..."
    lsof -ti:$ADMIN_PORT | xargs kill -9 2>/dev/null || echo "No process found on port $ADMIN_PORT"
    sleep 2
  fi
fi

# Check if node_modules exists, if not run npm install
if [ ! -d "node_modules" ]; then
  echo "Installing dependencies for Admin UI..."
  npm install
fi

# Start with timeout and error handling
echo "Executing npm start for Admin UI..."
npm start > admin-ui.log 2>&1 &
ADMIN_UI_PID=$!
cd ../..

# Check if process is still running after a short delay
sleep 3
if ps -p $ADMIN_UI_PID > /dev/null; then
  echo "Admin UI started with PID: $ADMIN_UI_PID on port $ADMIN_PORT"
else
  echo "Error: Admin UI failed to start. Check frontend/admin-ui/admin-ui.log for details."
  cat frontend/admin-ui/admin-ui.log | tail -20
  exit 1
fi

# 2. Start User UI
echo "Starting User UI..."
cd frontend/user-ui
# Create .env file if it doesn't exist
if [ ! -f .env ]; then
  echo "PORT=3001" > .env
  echo "Created .env file for User UI"
fi
# Get the port from .env file
USER_PORT=$(grep "PORT=" .env | cut -d '=' -f2)

# Check if port is already in use
if check_port $USER_PORT; then
  echo "Warning: Port $USER_PORT is already in use!"
  read -p "Would you like to use a different port? (y/n): " change_port
  if [ "$change_port" = "y" ]; then
    read -p "Enter new port number: " new_port
    sed -i '' "s/PORT=$USER_PORT/PORT=$new_port/" .env
    USER_PORT=$new_port
    echo "Updated User UI port to $USER_PORT"
  else
    echo "Attempting to kill the process using port $USER_PORT..."
    lsof -ti:$USER_PORT | xargs kill -9 2>/dev/null || echo "No process found on port $USER_PORT"
    sleep 2
  fi
fi

# Check if node_modules exists, if not run npm install
if [ ! -d "node_modules" ]; then
  echo "Installing dependencies for User UI..."
  npm install
fi

# Start with timeout and error handling
echo "Executing npm start for User UI..."
npm start > user-ui.log 2>&1 &
USER_UI_PID=$!
cd ../..

# Check if process is still running after a short delay
sleep 3
if ps -p $USER_UI_PID > /dev/null; then
  echo "User UI started with PID: $USER_UI_PID on port $USER_PORT"
else
  echo "Error: User UI failed to start. Check frontend/user-ui/user-ui.log for details."
  cat frontend/user-ui/user-ui.log | tail -20
  exit 1
fi

# Save PIDs to file
echo "$ADMIN_UI_PID $USER_UI_PID" > .frontend_pids

echo "===== Frontend Services Started ====="
echo "Admin UI: http://localhost:$ADMIN_PORT"
echo "User UI:  http://localhost:$USER_PORT"
echo ""
echo "To view frontend logs, use:"
echo "./tail-frontend-logs.sh admin   # For Admin UI logs only"
echo "./tail-frontend-logs.sh user    # For User UI logs only" 
echo "./tail-frontend-logs.sh         # For both logs"
echo ""
echo "To stop frontend services, run: ./stop-frontend.sh"