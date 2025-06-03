<#
.SYNOPSIS
    Backend Startup Script
.DESCRIPTION
    This script starts the backend components of the notification system:
    - Docker services (Kafka, Zookeeper, MailCrab)
    - Backend Spring Boot application
#>

Write-Host "===== Starting Backend Services =====" -ForegroundColor Cyan

# Function to check if a port is in use
function Test-PortInUse {
    param([int]$Port)
    $tcpClient = New-Object System.Net.Sockets.TcpClient
    try {
        $tcpClient.Connect("localhost", $Port)
        $tcpClient.Close()
        return $true
    } catch {
        return $false
    }
}

# 1. Start Docker Compose services
Write-Host "Starting Docker Compose services..."
try {
    docker compose up -d
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to start Docker Compose services"
    }
} catch {
    Write-Error "Failed to start Docker Compose services. Exiting."
    exit 1
}

# 2. Check and create Kafka topics if needed
Write-Host "Checking Kafka topics..."
$KAFKA_CONTAINER_NAME = "notification-sse-kafka-1"
$BROKER_LIST = "kafka:9093"

# Wait for Kafka to be ready
Write-Host -NoNewline "Waiting for Kafka to be ready..."
$MAX_KAFKA_RETRIES = 30
$KAFKA_RETRY_COUNT = 0

while ($true) {
    try {
        $result = docker exec $KAFKA_CONTAINER_NAME sh -c "kafka-topics.sh --bootstrap-server $BROKER_LIST --list" 2>$null
        if ($LASTEXITCODE -eq 0) { break }
    } catch {}
    
    if ($KAFKA_RETRY_COUNT -ge $MAX_KAFKA_RETRIES) {
        Write-Error "Kafka failed to start within the expected time. Exiting."
        exit 1
    }
    
    Write-Host -NoNewline "."
    Start-Sleep -Seconds 2
    $KAFKA_RETRY_COUNT++
}
Write-Host ""
Write-Host "Kafka is up and running!" -ForegroundColor Green

# Get list of existing topics
$EXISTING_TOPICS = docker exec $KAFKA_CONTAINER_NAME sh -c "kafka-topics.sh --bootstrap-server $BROKER_LIST --list"

# Check if all required topics exist
$REQUIRED_TOPICS = @("notifications", "critical-notifications", "broadcast-notifications")
$TOPICS_MISSING = $false

foreach ($TOPIC in $REQUIRED_TOPICS) {
    if ($EXISTING_TOPICS -notcontains $TOPIC) {
        $TOPICS_MISSING = $true
        break
    }
}

# Create topics only if some are missing
if ($TOPICS_MISSING) {
    Write-Host "Some Kafka topics are missing. Creating topics..."
    try {
        & "$PSScriptRoot\create_kafka_topics.ps1"
        if ($LASTEXITCODE -ne 0) { throw "Failed to create Kafka topics" }
    } catch {
        Write-Error "Failed to create Kafka topics. Exiting."
        exit 1
    }
} else {
    Write-Host "All required Kafka topics already exist. Skipping topic creation." -ForegroundColor Green
}

# Verify Kafka connectivity from host machine
Write-Host -NoNewline "Verifying Kafka connectivity from host machine..."
$MAX_CONNECT_RETRIES = 10
$CONNECT_RETRY_COUNT = 0

while (-not (Test-PortInUse -Port 9092)) {
    if ($CONNECT_RETRY_COUNT -ge $MAX_CONNECT_RETRIES) {
        Write-Error "Cannot connect to Kafka on localhost:9092. Exiting."
        exit 1
    }
    Write-Host -NoNewline "."
    Start-Sleep -Seconds 2
    $CONNECT_RETRY_COUNT++
}
Write-Host ""
Write-Host "Kafka is accessible from host machine!" -ForegroundColor Green

# 3. Start Backend
# At this point, all dependencies should be ready.
Write-Host "Starting Backend service in foreground. Press Ctrl+C to stop." -ForegroundColor Cyan
Set-Location "$PSScriptRoot\..\backend"

try {
    if ($IsWindows) {
        .\mvnw.cmd spring-boot:run
    } else {
        ./mvnw spring-boot:run
    }
} finally {
    Set-Location "$PSScriptRoot\.."
}

# The script will block here until the backend process exits.
Write-Host "Backend service stopped." -ForegroundColor Cyan
