#!/bin/bash

# Notification System Control Script
# This script provides a unified interface to start or stop all components

function show_usage {
  echo "Usage: $0 [backend|frontend|kafka-topics|docker-up|docker-down|fix-ui]"
  echo "  backend      - Start only backend services (use in a separate terminal)"
  echo "  frontend     - Start only frontend services (use in a separate terminal)"
  echo "  kafka-topics - Create Kafka topics (runs create-kafka-topics.sh)"
  echo "  docker-up    - Start Docker Compose services (docker-compose up -d)"
  echo "  docker-down  - Stop Docker Compose services (docker-compose down)"
  echo "  fix-ui       - Run the UI fix script (fix-ui.sh)"
  echo
  echo "NOTE: For development, run './notification-system.sh backend' and './notification-system.sh frontend' in separate terminals."
}

case "$1" in
  backend)
    echo "Starting backend services only..."
    ./scripts/start_backend.sh
    ;;
  frontend)
    echo "Starting frontend services only..."
    ./scripts/start_frontend.sh
    ;;
  kafka-topics)
    echo "Creating Kafka topics..."
    ./scripts/create_kafka_topics.sh
    ;;
  docker-up)
    echo "Starting Docker Compose services..."
    docker-compose up -d
    ;;
  docker-down)
    echo "Stopping Docker Compose services..."
    docker-compose down
    ;;
  fix-ui)
    echo "Running UI fix script..."
    ./scripts/fix_ui.sh
    ;;
  *)
    show_usage
    exit 1
    ;;
esac

exit 0