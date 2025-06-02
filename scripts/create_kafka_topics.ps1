<#
.SYNOPSIS
    Script to create Kafka topics required by the notification service
.DESCRIPTION
    Run this script after starting the docker-compose services to create
    all necessary Kafka topics for the notification system.
#>

$KAFKA_CONTAINER_NAME = "notification-sse-kafka-1" # Default name assigned by docker-compose
$BROKER_LIST = "kafka:9093" # Use the internal listener for commands run inside the container network

# List of topics to create
$TOPICS = @(
    "notifications"
    "critical-notifications"
    "broadcast-notifications"
)

# Check if Kafka container is running
$isContainerRunning = docker ps --format '{{.Names}}' | Select-String -Pattern "^${KAFKA_CONTAINER_NAME}$" -Quiet

if (-not $isContainerRunning) {
    Write-Error "Error: Kafka container '${KAFKA_CONTAINER_NAME}' is not running."
    Write-Host "Please start the services using 'docker compose up -d' first." -ForegroundColor Yellow
    exit 1
}

Write-Host -NoNewline "Waiting for Kafka broker to be ready..."
$counter = 0
$timeout = 60

while ($true) {
    try {
        $null = docker exec $KAFKA_CONTAINER_NAME kafka-topics.sh --bootstrap-server $BROKER_LIST --list 2>$null
        if ($LASTEXITCODE -eq 0) { break }
    } catch {}
    
    if ($counter -ge $timeout/2) {
        Write-Error "Kafka broker did not become ready within $timeout seconds."
        exit 1
    }
    
    Write-Host -NoNewline "."
    Start-Sleep -Seconds 2
    $counter += 2
}

Write-Host ""
Write-Host "Kafka broker is ready." -ForegroundColor Green
Write-Host "Creating Kafka topics..."

$existingTopics = @(docker exec $KAFKA_CONTAINER_NAME kafka-topics.sh --bootstrap-server $BROKER_LIST --list)

foreach ($TOPIC in $TOPICS) {
    if ($existingTopics -contains $TOPIC) {
        Write-Host " - Topic already exists: $TOPIC" -ForegroundColor Yellow
        continue
    }
    
    Write-Host " - Creating topic: $TOPIC"
    
    $createTopicCmd = @(
        "exec", $KAFKA_CONTAINER_NAME,
        "kafka-topics.sh",
        "--bootstrap-server", $BROKER_LIST,
        "--create",
        "--topic", $TOPIC,
        "--partitions", "1",
        "--replication-factor", "1"
    )
    
    try {
        docker $createTopicCmd
        if ($LASTEXITCODE -ne 0) {
            Write-Warning "Failed to create topic: $TOPIC"
        } else {
            Write-Host "   Successfully created topic: $TOPIC" -ForegroundColor Green
        }
    } catch {
        Write-Error "Error creating topic $TOPIC : $_"
    }
}

Write-Host "Kafka topic creation completed." -ForegroundColor Green
exit 0
