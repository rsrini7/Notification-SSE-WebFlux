#!/bin/bash

# Check if UI type is provided
if [ -z "$1" ]; then
  echo "Usage: ./fix-ui.sh [user|admin]"
  echo "  user  - Fix User UI issues"
  echo "  admin - Fix Admin UI issues"
  exit 1
fi

UI_TYPE="$1"
PORT=""
DIR_PATH=""

# Set variables based on UI type
if [ "$UI_TYPE" = "user" ]; then
  DIR_PATH="/Users/srini/Ws/notification-ws/frontend/user-ui"
  PORT="3001"
  echo "Fixing User UI issues..."
elif [ "$UI_TYPE" = "admin" ]; then
  DIR_PATH="/Users/srini/Ws/notification-ws/frontend/admin-ui"
  PORT="3000"
  echo "Fixing Admin UI issues..."
else
  echo "Invalid UI type. Use 'user' or 'admin'."
  exit 1
fi

# Change to the appropriate directory
cd "$DIR_PATH"

# Clear any cached files
echo "Clearing node_modules/.cache directory..."
rm -rf node_modules/.cache

# Ensure all dependencies are installed correctly
echo "Reinstalling dependencies..."
npm install

# Check for any issues with the package.json
echo "Checking package.json configuration..."
if ! grep -q "\"homepage\":" package.json; then
  # Add homepage if it doesn't exist
  sed -i '' 's/"private": true,/"private": true,\n  "homepage": ".",/g' package.json
  echo "Added homepage field to package.json"
fi

# Create a proper .env file
echo "Ensuring proper .env configuration..."
echo "PORT=$PORT" > .env
echo "BROWSER=none" >> .env
echo "SKIP_PREFLIGHT_CHECK=true" >> .env

echo "Fix completed. Please restart the frontend with:"
echo "./stop-frontend.sh"
echo "./start-frontend.sh"
echo ""
echo "To view the frontend logs after restarting, use:"
echo "./tail-frontend-logs.sh $UI_TYPE  # For $UI_TYPE UI logs only"
echo "./tail-frontend-logs.sh           # For both logs"