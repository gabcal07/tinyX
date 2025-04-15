#!/bin/bash

set -e

REGISTRY="registry.cri.epita.fr/ing/majeures/tc/info/student/2026/2025-epitweet-tinyx-21"

# Change to project root once
cd "$(dirname "$0")/../.."

echo "Building and pushing search service..."
echo "Building Docker image for home timeline service..."
docker build -t ${REGISTRY}/srvc-home-timeline:latest -f srvc-home-timeline/Dockerfile .

echo "Pushing image to registry..."
docker push ${REGISTRY}/srvc-home-timeline:latest

echo "Done!"
