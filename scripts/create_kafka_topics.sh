#!/bin/bash

# Script to create Kafka topics required by the notification service
# Run this script after starting the docker-compose services

KAFKA_CONTAINER_NAME="notification-sse-webflux-kafka-1" # Default name assigned by docker-compose
BROKER_LIST="kafka:9093" # Use the internal listener for commands run inside the container network

# List of topics to create
TOPICS=(
  "notifications"
  "critical-notifications"
)

# Check if Kafka container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${KAFKA_CONTAINER_NAME}$"; then
  echo "Error: Kafka container '${KAFKA_CONTAINER_NAME}' is not running."
  echo "Please start the services using 'docker compose up -d' first."
  exit 1
fi

echo "Waiting for Kafka broker to be ready..."
# Simple wait loop - adjust timeout as needed
counter=0
timeout=60
while ! docker exec $KAFKA_CONTAINER_NAME kafka-topics.sh --bootstrap-server $BROKER_LIST --list > /dev/null 2>&1; do
  sleep 2
  counter=$((counter+2))
  if [ $counter -ge $timeout ]; then
    echo "Error: Kafka broker did not become ready within $timeout seconds."
    exit 1
  fi
  echo -n "."
done
echo " Kafka broker is ready."

echo "Creating Kafka topics..."

for TOPIC in "${TOPICS[@]}"; do
  echo " - Creating topic: $TOPIC"
  docker exec $KAFKA_CONTAINER_NAME kafka-topics.sh \
    --bootstrap-server $BROKER_LIST \
    --create \
    --topic $TOPIC \
    --partitions 1 \
    --replication-factor 1 \
    --if-not-exists
done

echo "Kafka topics created successfully (or already exist)."
exit 0