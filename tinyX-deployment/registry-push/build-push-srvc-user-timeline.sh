#!/bin/bash

set -e

REGISTRY="registry.cri.epita.fr/ing/majeures/tc/info/student/2026/2025-epitweet-tinyx-21"

# Change to project root once
cd "$(dirname "$0")/../.."

echo "Building and pushing search service..."
echo "Building Docker image for user timeline service..."
docker build -t ${REGISTRY}/srvc-user-timeline:latest -f srvc-user-timeline/Dockerfile .

echo "Pushing image to registry..."
docker push ${REGISTRY}/srvc-user-timeline:latest

echo "Done!"
