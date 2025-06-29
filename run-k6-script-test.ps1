# Set working directory to the script's location to resolve relative paths
param(
    [string]$DeploymentType = "local"
)

Push-Location -Path "backend/perf"

try {
    # Check if users.json exists
    if (-not (Test-Path -Path "users.json")) {
        Write-Error "users.json not found in backend/perf. Please create it before running the script."
        exit 1
    }


Write-Host "Usage: .\run-k6-script-test.ps1 [-DeploymentType <k8s|local>]"
Write-Host "  -DeploymentType k8s: Runs k6 tests against the Kubernetes backend (http://backend.localhost/backend)"
Write-Host "  -DeploymentType local: Runs k6 tests against the local backend (http://localhost:8080) (default)"
Write-Host ""
Write-Host "Press Enter to continue, or Ctrl+C to exit."
Read-Host

$baseUrl = ""
if ($DeploymentType -eq "k8s") {
    $baseUrl = "http://backend.localhost/backend"
} else {
    $baseUrl = "http://localhost:8080"
}

# Run k6 script
k6 run --log-output=stdout --verbose -e BASE_URL=$baseUrl k6-script.js
} finally {
    # Always return to the original directory, even if the script is interrupted
    Pop-Location
}