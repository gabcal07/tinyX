#!/bin/bash

# Test script for repo-post service deployed in Kubernetes
# Set to true to use the ingress, false to use the NodePort
# Keep it to true because it is not good practice to use NodePort in production


USE_INGRESS=true
NAMESPACE="tinyx"

# Service URLs
INGRESS_URL="http://192.168.122.104"
NODEPORT_URL="http://192.168.122.104:30082"

# Set the active URL based on the USE_INGRESS setting
if [ "$USE_INGRESS" = true ]; then
  # When using ingress, both services are accessed through the same base URL
  USER_SERVICE_URL="$INGRESS_URL"
  REPO_POST_URL="$INGRESS_URL"
  POSTS_PATH="/posts" # With ingress, we use the ingress path
  FILES_PATH="/api/files" # Files API path
  OPENAPI_PATH="/posts/q/openapi" # OpenAPI through ingress
  echo "Using Ingress routing"
else
  # When using NodePort, user service is through ingress, repo-post through NodePort
  USER_SERVICE_URL="$INGRESS_URL"
  REPO_POST_URL="$NODEPORT_URL"
  POSTS_PATH="/posts" # Base path for posts API
  FILES_PATH="/api/files" # Files API path
  OPENAPI_PATH="/q/openapi" # Direct OpenAPI path
  echo "Using NodePort direct access"
fi

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Create test image file
TEST_IMAGE_PATH="/tmp/test_image.jpg"
echo "Creating test image file..."
convert -size 100x100 xc:blue $TEST_IMAGE_PATH 2>/dev/null
if [ $? -ne 0 ]; then
  echo "ImageMagick convert command failed. Creating a simple test file instead."
  echo "This is a test file" > $TEST_IMAGE_PATH
fi

echo "Testing repo-post service"
echo "------------------------"
echo "User Service URL: $USER_SERVICE_URL"
echo "Repo Post URL: $REPO_POST_URL"
echo "Namespace: $NAMESPACE"

# Test 1: Check if OpenAPI is available
echo -e "\n${GREEN}Test 1:${NC} Checking OpenAPI availability"
curl -s "$REPO_POST_URL$OPENAPI_PATH" > /dev/null

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✓ OpenAPI endpoint is available${NC}"
else
  echo -e "${RED}✗ OpenAPI endpoint is not available${NC}"
fi

# Test 2: Create a test user using the user-service API
echo -e "\n${GREEN}Test 2:${NC} Creating test user via API"
USER_NICKNAME="testuser_$(date +%s)"
CREATE_USER_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "{\"username\":\"$USER_NICKNAME\"}" $USER_SERVICE_URL/users/create)
echo "User created: $USER_NICKNAME"
echo "Response: $CREATE_USER_RESPONSE"

echo "Waiting 3 seconds for user creation to propagate..."
sleep 3

echo -e "\n${GREEN}Test 3:${NC} Creating a post"
METADATA="{\"authorUsername\":\"$USER_NICKNAME\",\"type\":\"ORIGINAL\",\"text\":\"Test post from script\"}"
echo "Using metadata: $METADATA"

POST_RESPONSE=$(curl -s -X POST \
  -F "metadata=$METADATA;type=application/json" \
  "$REPO_POST_URL$POSTS_PATH")

echo "Response: $POST_RESPONSE"

POST_ID=$(echo $POST_RESPONSE | grep -o '"postId":"[^"]*"' | cut -d'"' -f4)
if [ -n "$POST_ID" ]; then
  echo -e "${GREEN}✓ Post created with ID: $POST_ID${NC}"
  
  echo -e "\n${GREEN}Test 4:${NC} Retrieving post by ID"
  GET_RESPONSE=$(curl -s "$REPO_POST_URL$POSTS_PATH/$POST_ID")
  echo "Response: $GET_RESPONSE"
  
  echo -e "\n${GREEN}Test 5:${NC} Deleting post"
  DELETE_RESPONSE=$(curl -s -X POST -H "X-user-name: $USER_NICKNAME" "$REPO_POST_URL$POSTS_PATH/$POST_ID")
  echo "Response: $DELETE_RESPONSE"
else
  echo -e "${RED}✗ Failed to create post${NC}"
fi

# Create multiple posts with files for testing file handling
FILE_IDS=()

for i in {1..2}; do
  # Test with a file upload
  echo -e "\n${GREEN}Test 6.$i:${NC} Creating a post with media file $i"
  METADATA="{\"authorUsername\":\"$USER_NICKNAME\",\"type\":\"ORIGINAL\",\"text\":\"Test post with image $i\"}"

  POST_WITH_FILE_RESPONSE=$(curl -s -X POST \
    -F "metadata=$METADATA;type=application/json" \
    -F "file=@$TEST_IMAGE_PATH" \
    "$REPO_POST_URL$POSTS_PATH")

  echo "Response: $POST_WITH_FILE_RESPONSE"

  # Extract the postId and mediaUrl
  POST_WITH_FILE_ID=$(echo $POST_WITH_FILE_RESPONSE | grep -o '"postId":"[^"]*"' | cut -d'"' -f4)
  MEDIA_URL=$(echo $POST_WITH_FILE_RESPONSE | grep -o '"mediaUrl":"[^"]*"' | cut -d'"' -f4)
  
  if [ -n "$MEDIA_URL" ]; then
    # Extract the file ID from the mediaUrl
    FILE_ID=$(basename "$MEDIA_URL")
    FILE_IDS+=("$FILE_ID")
    echo -e "${GREEN}✓ Post with file created. File ID: $FILE_ID${NC}"
    
    # Test downloading the file
    echo -e "\n${GREEN}Test 7.$i:${NC} Downloading file $i"
    DOWNLOAD_PATH="/tmp/downloaded_file_$i"
    DOWNLOAD_RESPONSE=$(curl -s -o "$DOWNLOAD_PATH" "$REPO_POST_URL$FILES_PATH/$FILE_ID")
    
    if [ -f "$DOWNLOAD_PATH" ]; then
      FILE_SIZE=$(stat -c %s "$DOWNLOAD_PATH")
      echo "Downloaded file size: $FILE_SIZE bytes"
      if [ "$FILE_SIZE" -gt 0 ]; then
        echo -e "${GREEN}✓ File downloaded successfully${NC}"
      else
        echo -e "${RED}✗ Downloaded file is empty${NC}"
      fi
    else
      echo -e "${RED}✗ File download failed${NC}"
    fi
  fi
done

# Test 8: Clear all files
echo -e "\n${GREEN}Test 8:${NC} Testing file clearing endpoint"
CLEAR_RESPONSE=$(curl -s -X POST "$REPO_POST_URL$FILES_PATH/clear")
echo "Clear files response: $CLEAR_RESPONSE"

# Check if files are gone
ALL_FILES_GONE=true
for FILE_ID in "${FILE_IDS[@]}"; do
  echo "Checking if file $FILE_ID is gone..."
  STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$REPO_POST_URL$FILES_PATH/$FILE_ID")
  if [ "$STATUS_CODE" -eq "200" ]; then
    echo -e "${RED}✗ File $FILE_ID still exists (HTTP $STATUS_CODE)${NC}"
    ALL_FILES_GONE=false
  else
    echo -e "${GREEN}✓ File $FILE_ID is gone (HTTP $STATUS_CODE)${NC}"
  fi
done

if [ "$ALL_FILES_GONE" = true ]; then
  echo -e "${GREEN}✓ All files cleared successfully${NC}"
else
  echo -e "${RED}✗ Some files still exist${NC}"
fi

# Test 9: Delete all posts before deleting the user
echo -e "\n${GREEN}Test 9:${NC} Deleting all user's posts"
USER_POSTS_RESPONSE=$(curl -s "$REPO_POST_URL$POSTS_PATH/user/$USER_NICKNAME")
echo "User posts: $USER_POSTS_RESPONSE"

# Extract post IDs from the response
POST_IDS=$(echo $USER_POSTS_RESPONSE | grep -o '"postId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$POST_IDS" ]; then
  for POST_ID in $POST_IDS; do
    echo "Deleting post $POST_ID..."
    DELETE_RESPONSE=$(curl -s -X POST -H "X-user-name: $USER_NICKNAME" "$REPO_POST_URL$POSTS_PATH/$POST_ID")
    if [ $? -eq 0 ]; then
      echo -e "${GREEN}✓ Post $POST_ID deleted${NC}"
    else
      echo -e "${RED}✗ Failed to delete post $POST_ID${NC}"
    fi
  done
fi

# Test 10: Clean up - delete the test user
echo -e "\n${GREEN}Test 10:${NC} Deleting test user"
DELETE_USER_RESPONSE=$(curl -s -X DELETE "$USER_SERVICE_URL/users/delete/$USER_NICKNAME")
echo "Response: $DELETE_USER_RESPONSE"

# Clean up the test files
rm -f $TEST_IMAGE_PATH
for i in {1..2}; do
  rm -f "/tmp/downloaded_file_$i"
done

echo -e "\n${GREEN}All tests completed${NC}"