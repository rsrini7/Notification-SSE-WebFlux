# This script builds the Docker images and deploys the notification system to Kubernetes.

param(
    [switch]$EnablePortForwarding = $false
)

# Deploy the infrastructure (Zookeeper and Kafka)
echo "Deploying infrastructure..."
kubectl apply -k k8s/infra

# Build the backend image
echo "Building backend image..."
docker build -t backend:latest ./backend

# Build the admin-ui image
echo "Building admin-ui image..."
docker build -t admin-ui:latest ./frontend/admin-ui

# Build the user-ui image
echo "Building user-ui image..."
docker build -t user-ui:latest ./frontend/user-ui

# Apply the kustomization configuration from the k8s/app directory.
# This will create all the deployments and services defined in the app.
kubectl apply -k k8s/app

# You can check the status of the rollout with the following command:
kubectl rollout status deployment/backend
kubectl rollout status deployment/admin-ui
kubectl rollout status deployment/user-ui

# To access the services, you might need to set up port forwarding. For example:
function Start-PortForward {
    param (
        [string]$Service,
        [string]$LocalPort,
        [string]$RemotePort
    )
    $command = "kubectl port-forward service/$Service ${LocalPort}:${RemotePort}"
    # Check if the local port is already in use
    # This is a more robust way to check if the port-forwarding is active
    $portInUse = (Get-NetTCPConnection -LocalPort $LocalPort -ErrorAction SilentlyContinue | Where-Object { $_.State -eq "Listen" -or $_.State -eq "Established" })

    if (-not $portInUse) {
        Write-Host "Starting port-forward for $Service (${LocalPort}:${RemotePort})..."
        Start-Process powershell -ArgumentList "-NoExit", "-Command", $command
    } else {
        Write-Host "Port ${LocalPort} is already in use. Assuming port-forward for $Service is running. Skipping."
    }
}

if ($EnablePortForwarding) {
    Write-Host "Starting port-forwarding for services..."
    Start-PortForward -Service "kafka-headless" -LocalPort "9092" -RemotePort "9092"
    Start-PortForward -Service "backend" -LocalPort "8080" -RemotePort "8080"
    Start-PortForward -Service "mailcrab" -LocalPort "1080" -RemotePort "1080"
    Start-PortForward -Service "mailcrab" -LocalPort "1025" -RemotePort "1025"
    Start-PortForward -Service "admin-ui" -LocalPort "3000" -RemotePort "80"
    Start-PortForward -Service "user-ui" -LocalPort "3001" -RemotePort "80"
} else {
    Write-Host "Port forwarding is disabled. To enable, run with -EnablePortForwarding."
}