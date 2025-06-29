# Set working directory to the script's location to resolve relative paths
Push-Location -Path "backend/perf"

try {
    # Check if users.json exists
    if (-not (Test-Path -Path "users.json")) {
        Write-Error "users.json not found in backend/perf. Please create it before running the script."
        exit 1
    }

    # Run k6 script
    k6 run --log-output=stdout --verbose -e BASE_URL=http://backend.localhost/backend k6-script.js
} finally {
    # Always return to the original directory, even if the script is interrupted
    Pop-Location
}