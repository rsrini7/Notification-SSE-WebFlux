# Set working directory to the script's location to resolve relative paths
Push-Location -Path "backend/perf"

# Check if users.json exists
if (-not (Test-Path -Path "users.json")) {
    Write-Error "users.json not found in backend/perf. Please create it before running the script."
    Pop-Location
    exit 1
}

# Run k6 script
k6 run --log-output=stdout --verbose -e BASE_URL=http://localhost:8080 k6-script.js

# Return to the original directory
Pop-Location