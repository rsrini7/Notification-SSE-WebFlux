# Undeploy Kubernetes infrastructure and applications

Write-Host "Undeploying Kubernetes applications..."
kubectl delete -k k8s/base

Write-Host "Undeploying Kubernetes infrastructure..."
kubectl delete -k k8s/infra

Write-Host "Kubernetes undeployment complete."