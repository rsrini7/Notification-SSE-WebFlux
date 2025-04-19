#!/bin/bash

# Frontend Startup Script
# This script starts the frontend components of the notification system:
# - Admin UI
# - User UI

echo "===== Starting Frontend Services ====="

# 1. Ensure concurrently is installed in frontend/
echo "Ensuring 'concurrently' is installed in frontend..."
cd frontend
if [ ! -d "node_modules/concurrently" ]; then
  npm install concurrently --no-audit --no-fund
fi

# 2. Ensure admin-ui and user-ui dependencies are installed
for dir in admin-ui user-ui; do
  if [ ! -d "$dir/node_modules" ]; then
    echo "Installing dependencies for $dir..."
    cd $dir
    npm install --no-audit --no-fund
    cd ..
  fi
done

# 3. Run both UIs in parallel using npm script from frontend/package.json
cd frontend
npm start &
FRONTEND_PID=$!
cd ..

# Wait for both ports to be available, then open both UIs in browser
wait_for_port() {
  local port=$1
  local retries=30
  local count=0
  while ! nc -z localhost $port; do
    sleep 1
    count=$((count+1))
    if [ $count -ge $retries ]; then
      echo "Timeout waiting for port $port"
      return 1
    fi
  done
  return 0
}

wait_for_port 3000 && wait_for_port 3001

if [[ "$OSTYPE" == darwin* ]]; then
    open "http://localhost:3000"
    open "http://localhost:3001"
    open "http://localhost:1080"
elif [[ "$OSTYPE" == linux-gnu* ]]; then
    xdg-open "http://localhost:3000" &>/dev/null || true
    xdg-open "http://localhost:3001" &>/dev/null || true
    xdg-open "http://localhost:1080" &>/dev/null || true
elif [[ "$OSTYPE" == msys* ]] || [[ "$OSTYPE" == cygwin* ]]; then
    start "http://localhost:3000" || true
    start "http://localhost:3001" || true
    start "http://localhost:1080" || true
fi

wait $FRONTEND_PID

echo ""
echo "MailCrab: http://localhost:1080"
echo ""
echo "Both frontend logs will be shown below. Use Ctrl+C to stop both UIs."
echo ""
