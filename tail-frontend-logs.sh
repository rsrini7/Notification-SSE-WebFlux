#!/bin/bash

# Script to tail frontend logs
# Usage: ./tail-frontend-logs.sh [admin|user|all]

ADMIN_LOG="frontend/admin-ui/admin-ui.log"
USER_LOG="frontend/user-ui/user-ui.log"

function show_usage {
  echo "Usage: $0 [admin|user|all]"
  echo "  admin - Show Admin UI logs"
  echo "  user  - Show User UI logs"
  echo "  all   - Show both Admin and User UI logs (default)"
}

function check_log_exists {
  if [ ! -f "$1" ]; then
    echo "Log file $1 does not exist. Make sure the frontend is running."
    exit 1
  fi
}

case "$1" in
  admin)
    check_log_exists "$ADMIN_LOG"
    echo "Tailing Admin UI logs (Ctrl+C to exit)..."
    tail -f "$ADMIN_LOG"
    ;;
  user)
    check_log_exists "$USER_LOG"
    echo "Tailing User UI logs (Ctrl+C to exit)..."
    tail -f "$USER_LOG"
    ;;
  all|"")
    # Check if both log files exist
    if [ ! -f "$ADMIN_LOG" ] && [ ! -f "$USER_LOG" ]; then
      echo "No log files found. Make sure the frontends are running."
      exit 1
    fi
    
    # Use multitail if available, otherwise use separate terminals
    if command -v multitail &> /dev/null; then
      echo "Tailing both Admin and User UI logs (Ctrl+C to exit)..."
      multitail -cs 2 "$ADMIN_LOG" "$USER_LOG"
    else
      echo "Tailing both Admin and User UI logs in separate windows (Ctrl+C to exit)..."
      # Open new terminal window for Admin UI logs if the file exists
      if [ -f "$ADMIN_LOG" ]; then
        osascript -e "tell app \"Terminal\" to do script \"cd $(pwd) && echo 'Admin UI Logs:' && tail -f $ADMIN_LOG\""
      fi
      
      # Show User UI logs in current terminal if the file exists
      if [ -f "$USER_LOG" ]; then
        echo "User UI Logs:"
        tail -f "$USER_LOG"
      fi
    fi
    ;;
  *)
    show_usage
    exit 1
    ;;
esac