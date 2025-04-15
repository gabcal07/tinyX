#!/bin/bash

set -e

REGISTRY="registry.cri.epita.fr/ing/majeures/tc/info/student/2026/2025-epitweet-tinyx-21"

cd "$(dirname "$0")/../.."

echo "Building Docker image for social service..."
docker build -t ${REGISTRY}/repo-social:latest -f repo-social/Dockerfile .

echo "Pushing image to registry..."
docker push ${REGISTRY}/repo-social:latest

echo "Done!"