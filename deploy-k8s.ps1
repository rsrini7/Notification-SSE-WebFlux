# This script builds the Docker images and deploys the notification system to Kubernetes.

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

# Apply the kustomization configuration from the k8s/base directory.
# This will create all the deployments and services defined in the base.
kubectl apply -k k8s/base

# You can check the status of the rollout with the following command:
# kubectl rollout status deployment/backend
# kubectl rollout status deployment/admin-ui
# kubectl rollout status deployment/user-ui

# To access the services, you might need to set up port forwarding. For example:
# kubectl port-forward service/backend 8080:8080
# kubectl port-forward service/admin-ui 8081:80
# kubectl port-forward service/user-ui 8082:80