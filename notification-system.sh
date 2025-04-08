#!/bin/bash

# Notification System Control Script
# This script provides a unified interface to start or stop all components

function show_usage {
  echo "Usage: $0 [start|stop|restart|backend|frontend|logs]"
  echo "  start    - Start all components (backend and frontend)"
  echo "  stop     - Stop all components (backend and frontend)"
  echo "  restart  - Restart all components"
  echo "  backend  - Start only backend services"
  echo "  frontend - Start only frontend services"
  echo "  logs     - View frontend logs (admin, user, or both)"
}

case "$1" in
  start)
    echo "Starting all notification system components..."
    ./start-backend.sh
    ./start-frontend.sh
    ;;
  stop)
    echo "Stopping all notification system components..."
    ./stop-frontend.sh
    ./stop-backend.sh
    ;;
  restart)
    echo "Restarting all notification system components..."
    ./stop-frontend.sh
    ./stop-backend.sh
    ./start-backend.sh
    ./start-frontend.sh
    ;;
  backend)
    echo "Starting backend services only..."
    ./start-backend.sh
    ;;
  frontend)
    echo "Starting frontend services only..."
    ./start-frontend.sh
    ;;
  logs)
    if [ "$2" = "admin" ]; then
      echo "Showing Admin UI logs..."
      ./tail-frontend-logs.sh admin
    elif [ "$2" = "user" ]; then
      echo "Showing User UI logs..."
      ./tail-frontend-logs.sh user
    else
      echo "Showing all frontend logs..."
      ./tail-frontend-logs.sh
    fi
    ;;
  *)
    show_usage
    exit 1
    ;;
esac

exit 0