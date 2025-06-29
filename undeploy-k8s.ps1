# Undeploy Kubernetes infrastructure and applications

Write-Host "Undeploying Kubernetes applications..."
kubectl delete -k k8s/app

Write-Host "Undeploying Kubernetes infrastructure..."
kubectl delete -k k8s/infra

Write-Host "Kubernetes undeployment complete."