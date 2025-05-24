<#
.SYNOPSIS
    Frontend Startup Script
.DESCRIPTION
    This script starts the frontend components of the notification system:
    - Admin UI
    - User UI
#>


Write-Host "=== MailCrab Available in port: 1080 ===" -ForegroundColor Green

Write-Host "===== Starting Frontend Services =====" -ForegroundColor Cyan

# 1. Ensure concurrently is installed in frontend/
Write-Host "Ensuring 'concurrently' is installed in frontend..."
Set-Location "$PSScriptRoot\..\frontend"

if (-not (Test-Path "node_modules\concurrently")) {
    npm install concurrently --no-audit --no-fund
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to install concurrently. Exiting."
        exit 1
    }
}

# 2. Ensure admin-ui and user-ui dependencies are installed
foreach ($dir in @("admin-ui", "user-ui")) {
    if (-not (Test-Path "$dir\node_modules")) {
        Write-Host "Installing dependencies for $dir..."
        Set-Location $dir
        npm install --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to install dependencies for $dir. Exiting."
            exit 1
        }
        Set-Location "$PSScriptRoot\..\frontend"
    }
}

# 3. Run both UIs in parallel using npm script from frontend/package.json
Write-Host "Starting frontend services..." -ForegroundColor Cyan

# Change to frontend directory
Set-Location "$PSScriptRoot\..\frontend"

# Run npm start
Write-Host "Running 'npm start' in the frontend directory..." -ForegroundColor Green
npm start

# If we get here, npm start has exited
Write-Host "Frontend services have stopped." -ForegroundColor Yellow
