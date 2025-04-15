#!/bin/bash

set -e

REGISTRY="registry.cri.epita.fr/ing/majeures/tc/info/student/2026/2025-epitweet-tinyx-21"

# Change to project root once
cd "$(dirname "$0")/.."

echo "Building and pushing user service..."
echo "Building Docker image for user service..."
docker build -t ${REGISTRY}/srvc-user:latest -f srvc-user/Dockerfile .

echo "Pushing image to registry..."
docker push ${REGISTRY}/srvc-user:latest

echo "Done!"

echo "Building and pushing post service..."
echo "Building Docker image for post service..."
docker build -t ${REGISTRY}/repo-post:latest -f repo-post/Dockerfile .

echo "Pushing image to registry..."
docker push ${REGISTRY}/repo-post:latest

echo "Building and pushing social service..."
echo "Building Docker image for social service..."
docker build -t ${REGISTRY}/repo-social:latest -f repo-social/Dockerfile .

echo "Pushing image to registry..."
docker push ${REGISTRY}/repo-social:latest

echo "Done!"
