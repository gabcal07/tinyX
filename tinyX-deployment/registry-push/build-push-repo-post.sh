#!/bin/bash

set -e

REGISTRY="registry.cri.epita.fr/ing/majeures/tc/info/student/2026/2025-epitweet-tinyx-21"

# Change to project root once
cd "$(dirname "$0")/../.."

echo "Building Docker image for post service..."
docker build -t ${REGISTRY}/repo-post:latest -f repo-post/Dockerfile .

echo "Pushing image to registry..."
docker push ${REGISTRY}/repo-post:latest

echo "Done!"