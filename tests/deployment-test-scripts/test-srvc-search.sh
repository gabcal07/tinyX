#!/bin/bash

# Test script for search service deployed in Kubernetes

# Set to true to use the ingress, false to use the NodePort
USE_INGRESS=true
NAMESPACE="tinyx"

# Service URLs
INGRESS_URL="http://192.168.122.104"
NODEPORT_URL="http://192.168.122.104:30084" # Adjust this port if your search service uses a different NodePort

# Set the active URL based on the USE_INGRESS setting
if [ "$USE_INGRESS" = true ]; then
  SEARCH_URL="$INGRESS_URL"
  USER_SERVICE_URL="$INGRESS_URL"
  REPO_POST_URL="$INGRESS_URL"
  SEARCH_PATH="/search" 
  POSTS_PATH="/posts"
  USERS_PATH="/users"
  echo "Using Ingress routing"
else
  SEARCH_URL="$NODEPORT_URL"
  USER_SERVICE_URL="$INGRESS_URL"
  REPO_POST_URL="$INGRESS_URL/posts"
  SEARCH_PATH="/search"
  POSTS_PATH="/posts"
  USERS_PATH="/users"
  echo "Using NodePort direct access"
fi

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

echo "Testing search service"
echo "------------------------"
echo "Search URL: $SEARCH_URL"
echo "User Service URL: $USER_SERVICE_URL"
echo "Post Service URL: $REPO_POST_URL"
echo "Namespace: $NAMESPACE"

# Test 1: Check if the search endpoint responds (even if empty results)
echo -e "\n${GREEN}Test 1:${NC} Checking search endpoint availability"
CHECK_RESPONSE=$(curl -s "$SEARCH_URL$SEARCH_PATH/?query=test" -o /dev/null -w "%{http_code}")

if [ "$CHECK_RESPONSE" -eq 200 ]; then
  echo -e "${GREEN}✓ Search endpoint is available${NC}"
else
  echo -e "${RED}✗ Search endpoint is not available (HTTP $CHECK_RESPONSE)${NC}"
fi

# Test 2: Check that empty query parameter returns 400 Bad Request
echo -e "\n${GREEN}Test 2:${NC} Checking empty query parameter validation"
EMPTY_QUERY_RESPONSE=$(curl -s "$SEARCH_URL$SEARCH_PATH/" -o /dev/null -w "%{http_code}")

if [ "$EMPTY_QUERY_RESPONSE" -eq 400 ]; then
  echo -e "${GREEN}✓ Empty query parameter returns 400 Bad Request as expected${NC}"
else
  echo -e "${RED}✗ Empty query parameter should return 400 Bad Request but got $EMPTY_QUERY_RESPONSE${NC}"
fi

# Test 3: Create a test user
echo -e "\n${GREEN}Test 3:${NC} Creating a test user"
TEST_USER="search_test_$(date +%s)"
echo "Creating user: $TEST_USER"
USER_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "{\"username\":\"$TEST_USER\"}" "$USER_SERVICE_URL$USERS_PATH/create")
echo "User creation response: $USER_RESPONSE"

# Wait for user creation to propagate
echo "Waiting 5 seconds for user creation to propagate..."
sleep 5

# Verify the user exists before proceeding
echo "Verifying user was created..."
USER_CHECK=$(curl -s "$USER_SERVICE_URL$USERS_PATH/get/$TEST_USER")
echo "User check response: $USER_CHECK"

if echo "$USER_CHECK" | grep -q "$TEST_USER"; then
  echo -e "${GREEN}✓ User $TEST_USER was successfully created and verified${NC}"
else
  echo -e "${RED}✗ User verification failed, might cause post creation to fail${NC}"
  echo "Waiting 5 more seconds before attempting to create posts..."
  sleep 5
fi

# Test 4: Create test posts with specific content
echo -e "\n${GREEN}Test 4:${NC} Creating test posts with searchable content"

# Create post with a unique keyword
UNIQUE_KEYWORD="uniquesearchterm$(date +%s)"
echo "Using unique search keyword: $UNIQUE_KEYWORD"

# Create a single post 
POST_METADATA="{\"authorUsername\":\"$TEST_USER\",\"type\":\"ORIGINAL\",\"text\":\"Search test post with keyword $UNIQUE_KEYWORD\"}"
echo "Creating post with metadata: $POST_METADATA"

echo "Creating post..."
POST_RESPONSE=$(curl -s -X POST -F "metadata=$POST_METADATA;type=application/json" "$REPO_POST_URL$POSTS_PATH")
echo "Post response: $POST_RESPONSE"

if echo "$POST_RESPONSE" | grep -q "does not exist"; then
  echo -e "${RED}✗ Post creation failed: Author not recognized${NC}"
  
  # Try with another test user
  echo "Creating a post with a direct test user..."
  TEST_USER="test_direct"
  DIRECT_POST_METADATA="{\"authorUsername\":\"$TEST_USER\",\"type\":\"ORIGINAL\",\"text\":\"Search test post with keyword $UNIQUE_KEYWORD\"}"
  DIRECT_POST_RESPONSE=$(curl -s -X POST -F "metadata=$DIRECT_POST_METADATA;type=application/json" "$REPO_POST_URL$POSTS_PATH")
  echo "Direct test post response: $DIRECT_POST_RESPONSE"
  
  POST_ID=$(echo "$DIRECT_POST_RESPONSE" | grep -o '"postId":"[^"]*"' | cut -d'"' -f4)
  if [ -n "$POST_ID" ]; then
    echo -e "${GREEN}✓ Fallback post created with ID: $POST_ID${NC}"
  else
    echo -e "${RED}✗ Post creation failed even with fallback user${NC}"
  fi
else
  POST_ID=$(echo "$POST_RESPONSE" | grep -o '"postId":"[^"]*"' | cut -d'"' -f4)
  if [ -n "$POST_ID" ]; then
    echo -e "${GREEN}✓ Post created successfully with ID: $POST_ID${NC}"
  fi
fi

# Wait for posts to be indexed in Elasticsearch
echo "Waiting 10 seconds for posts to be indexed in Elasticsearch..."
sleep 10

# Test 5: Search for the unique keyword
echo -e "\n${GREEN}Test 5:${NC} Searching for posts with the unique keyword"
SEARCH_RESPONSE=$(curl -s "$SEARCH_URL$SEARCH_PATH/?query=$UNIQUE_KEYWORD")
echo "Search response: $SEARCH_RESPONSE"

# Check if we have any posts to search for
if [ -z "$POST_ID" ]; then
  echo -e "${YELLOW}⚠ No posts were successfully created, search test may be invalid${NC}"
else
  # Check if the search response contains at least one of our posts with the unique keyword
  if echo "$SEARCH_RESPONSE" | grep -q "$POST_ID"; then
    echo -e "${GREEN}✓ Search results include posts with the unique keyword${NC}"
  else
    echo -e "${RED}✗ Search results do not include posts with the unique keyword${NC}"
    
    # Check if search returned any results
    if [ "$SEARCH_RESPONSE" = "[]" ]; then
      echo "Search returned empty results. Possible indexing delay or search service issue."
    else
      echo "Search returned different results than expected. Possible mismatch in search algorithm."
    fi
  fi
fi

# Test 6: Search for a term that should return no results
echo -e "\n${GREEN}Test 6:${NC} Searching for a term that should return no results"
NO_RESULTS_SEARCH=$(curl -s "$SEARCH_URL$SEARCH_PATH/?query=thisshouldreallynotmatchanything$(date +%s)")
echo "No results search response: $NO_RESULTS_SEARCH"

# Check if the search response is an empty array
if [ "$NO_RESULTS_SEARCH" = "[]" ]; then
  echo -e "${GREEN}✓ Search for non-existent term returns empty array${NC}"
else
  echo -e "${RED}✗ Search for non-existent term should return empty array${NC}"
fi

# Test 7: Cleanup - delete the test posts
echo -e "\n${GREEN}Test 7:${NC} Cleaning up - deleting test posts"

if [ -n "$POST_ID" ]; then
  echo "Deleting post..."
  DELETE_POST=$(curl -s -X POST -H "X-user-name: $TEST_USER" "$REPO_POST_URL$POSTS_PATH/$POST_ID")
  echo "Delete post response: $DELETE_POST"
fi

# Test 8: Cleanup - delete the test user
echo -e "\n${GREEN}Test 8:${NC} Cleaning up - deleting test user"
DELETE_USER_RESPONSE=$(curl -s -X DELETE "$USER_SERVICE_URL$USERS_PATH/delete/$TEST_USER")
echo "Delete user response: $DELETE_USER_RESPONSE"

echo -e "\n${GREEN}All tests completed${NC}"

