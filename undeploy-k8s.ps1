# Undeploy Kubernetes infrastructure and applications

Write-Host "Undeploying Kubernetes applications..."
$appDeleteResult = kubectl delete -k k8s/app 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to undeploy Kubernetes applications from k8s/app. Exit code: $LASTEXITCODE. Error: $appDeleteResult"
    exit 1
} else {
    Write-Host $appDeleteResult
}

Write-Host "Undeploying Kubernetes infrastructure..."
$infraDeleteResult = kubectl delete -k k8s/infra 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to undeploy Kubernetes infrastructure from k8s/infra. Exit code: $LASTEXITCODE. Error: $infraDeleteResult"
    exit 1
} else {
    Write-Host $infraDeleteResult
}

Write-Host "Kubernetes undeployment complete."