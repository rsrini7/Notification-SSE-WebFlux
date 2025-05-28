<#
.SYNOPSIS
    Script to fix UI issues for the notification system
.DESCRIPTION
    This script helps fix common UI issues for both User and Admin interfaces
    by clearing caches, reinstalling dependencies, and ensuring proper configuration.
.PARAMETER UIType
    Specifies which UI to fix. Valid values are 'user' or 'admin'.
.EXAMPLE
    .\fix_ui.ps1 user
    Fixes the User UI issues
.EXAMPLE
    .\fix_ui.ps1 admin
    Fixes the Admin UI issues
#>

param(
    [Parameter(Mandatory=$true, Position=0)]
    [ValidateSet('user', 'admin')]
    [string]$UIType
)

# Set variables based on UI type
$PORT = if ($UIType -eq 'user') { "3001" } else { "3000" }
$UI_NAME = if ($UIType -eq 'user') { "User UI" } else { "Admin UI" }

# Get the base directory (two levels up from the scripts directory)
$baseDir = Split-Path -Parent $PSScriptRoot
$DIR_PATH = Join-Path $baseDir "frontend\$($UIType)-ui"

# Validate directory exists
if (-not (Test-Path $DIR_PATH)) {
    Write-Error "Error: Directory not found: $DIR_PATH"
    Write-Host "Please ensure you're running this script from the correct location." -ForegroundColor Yellow
    exit 1
}

Write-Host "Fixing $UI_NAME issues..." -ForegroundColor Cyan
Write-Host "Working directory: $DIR_PATH"

# Change to the appropriate directory
Push-Location $DIR_PATH

try {
    # Clear any cached files
    Write-Host "Clearing node_modules/.cache directory..."
    $cachePath = Join-Path $DIR_PATH "node_modules\.cache"
    if (Test-Path $cachePath) {
        Remove-Item -Path $cachePath -Recurse -Force -ErrorAction Stop
        Write-Host "Cache cleared successfully." -ForegroundColor Green
    } else {
        Write-Host "No cache directory found, skipping..." -ForegroundColor Yellow
    }

    # Ensure all dependencies are installed correctly
    Write-Host "Reinstalling dependencies..."
    npm install --no-audit --no-fund
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to install dependencies"
    }

    # Check for any issues with the package.json
    Write-Host "Checking package.json configuration..."
    $packageJsonPath = Join-Path $DIR_PATH "package.json"
    $packageJson = Get-Content $packageJsonPath -Raw | ConvertFrom-Json
    
    $needsUpdate = $false
    
    # Add homepage if it doesn't exist
    if (-not $packageJson.PSObject.Properties['homepage']) {
        $packageJson | Add-Member -MemberType NoteProperty -Name 'homepage' -Value '.' -Force
        $needsUpdate = $true
    }
    
    # Save changes if needed
    if ($needsUpdate) {
        $packageJson | ConvertTo-Json -Depth 10 | Set-Content -Path $packageJsonPath -Encoding UTF8
        Write-Host "Updated package.json with required configurations." -ForegroundColor Green
    } else {
        Write-Host "package.json is properly configured." -ForegroundColor Green
    }

    # Create a proper .env file
    Write-Host "Ensuring proper .env configuration..."
    @"
PORT=$PORT
BROWSER=none
SKIP_PREFLIGHT_CHECK=true
"@ | Set-Content -Path (Join-Path $DIR_PATH ".env") -Encoding UTF8

    Write-Host ""
    Write-Host "Fix completed successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "To complete the process:"
    Write-Host "1. Stop any running frontend services"
    Write-Host "2. Restart the frontend with: .\start_frontend.ps1"
    Write-Host ""
    Write-Host "To view the frontend logs after restarting, use:"
    Write-Host ".\tail_frontend_logs.ps1 $UIType  # For $UI_NAME logs only"
    Write-Host ".\tail_frontend_logs.ps1         # For both logs"
    
} catch {
    Write-Error "An error occurred: $_"
    Write-Host "Fix process failed. Please check the error message above." -ForegroundColor Red
    exit 1
} finally {
    # Return to the original directory
    Pop-Location
}

exit 0
