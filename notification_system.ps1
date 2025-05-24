#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Notification System Control Script
.DESCRIPTION
    This script provides a unified interface to start or stop all components of the notification system.
    Compatible with both Windows PowerShell 5.1 and PowerShell 7.5+.
.PARAMETER Action
    The action to perform. Valid values are:
    - backend      - Start only backend services (use in a separate terminal)
    - frontend     - Start only frontend services (use in a separate terminal)
    - kafka-topics - Create Kafka topics (runs create-kafka-topics.ps1)
    - docker-up    - Start Docker Compose services (docker-compose up -d)
    - docker-down  - Stop Docker Compose services (docker-compose down)
    - fix-ui       - Run the UI fix script (fix-ui.ps1)
.EXAMPLE
    .\notification_system.ps1 backend
    .\notification_system.ps1 frontend
    .\notification_system.ps1 kafka-topics
#>

param(
    [Parameter(Position=0)]
    [ValidateSet('backend', 'frontend', 'kafka-topics', 'docker-up', 'docker-down', 'fix-ui')]
    [string]$Action
)

function Show-Usage {
    Write-Host "Usage: .\$($MyInvocation.MyCommand.Name) [backend|frontend|kafka-topics|docker-up|docker-down|fix-ui]"
    Write-Host "  backend      - Start only backend services (use in a separate terminal)"
    Write-Host "  frontend     - Start only frontend services (use in a separate terminal)"
    Write-Host "  kafka-topics - Create Kafka topics (runs create-kafka-topics.ps1)"
    Write-Host "  docker-up    - Start Docker Compose services (docker-compose up -d)"
    Write-Host "  docker-down  - Stop Docker Compose services (docker-compose down)"
    Write-Host "  fix-ui       - Run the UI fix script (fix-ui.ps1)"
    Write-Host ""
    Write-Host "NOTE: For development, run '.\$($MyInvocation.MyCommand.Name) backend' and '.\$($MyInvocation.MyCommand.Name) frontend' in separate terminals."
}

# Main script execution
if (-not $Action) {
    Show-Usage
    exit 1
}

switch ($Action) {
    'backend' {
        Write-Host "Starting backend services only..."
        & "$PSScriptRoot\scripts\start_backend.ps1"
    }
    'frontend' {
        Write-Host "Starting frontend services only..."
        & "$PSScriptRoot\scripts\start_frontend.ps1"
    }
    'kafka-topics' {
        Write-Host "Creating Kafka topics..."
        & "$PSScriptRoot\scripts\create_kafka_topics.ps1"
    }
    'docker-up' {
        Write-Host "Starting Docker Compose services..."
        docker-compose up -d
    }
    'docker-down' {
        Write-Host "Stopping Docker Compose services..."
        docker-compose down
    }
    'fix-ui' {
        Write-Host "Running UI fix script..."
        & "$PSScriptRoot\scripts\fix_ui.ps1"
    }
    default {
        Show-Usage
        exit 1
    }
}

exit 0
