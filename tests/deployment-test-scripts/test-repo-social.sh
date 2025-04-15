#!/bin/bash

# Test script for repo-social service deployed in Kubernetes
# Set to true to use the ingress, false to use the NodePort
# Keep it to true because it is not good practice to use NodePort in production

USE_INGRESS=true
NAMESPACE="tinyx"

# Service URLs
INGRESS_URL="http://192.168.122.104"
NODEPORT_URL="http://192.168.122.104:30083" # Adjust this port if your social service uses a different NodePort

# Set the active URL based on the USE_INGRESS setting
if [ "$USE_INGRESS" = true ]; then
  USER_SERVICE_URL="$INGRESS_URL"
  REPO_POST_URL="$INGRESS_URL"
  REPO_SOCIAL_URL="$INGRESS_URL"
  SOCIAL_PATH="/social" # With ingress, we use the ingress path
  POSTS_PATH="/posts" # Posts API path
  OPENAPI_PATH="/social/q/openapi" # OpenAPI through ingress
  echo "Using Ingress routing"
else
  USER_SERVICE_URL="$INGRESS_URL"
  REPO_POST_URL="$INGRESS_URL/posts"
  REPO_SOCIAL_URL="$NODEPORT_URL"
  SOCIAL_PATH="/social" # Base path for social API
  POSTS_PATH="/posts" # Posts API path
  OPENAPI_PATH="/q/openapi" # Direct OpenAPI path
  echo "Using NodePort direct access"
fi

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "Testing repo-social service"
echo "------------------------"
echo "User Service URL: $USER_SERVICE_URL"
echo "Repo Post URL: $REPO_POST_URL"
echo "Repo Social URL: $REPO_SOCIAL_URL"
echo "Namespace: $NAMESPACE"

# Test 1: Check if OpenAPI is available
echo -e "\n${GREEN}Test 1:${NC} Checking OpenAPI availability"
curl -s "$REPO_SOCIAL_URL$OPENAPI_PATH" > /dev/null

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✓ OpenAPI endpoint is available${NC}"
else
  echo -e "${RED}✗ OpenAPI endpoint is not available${NC}"
fi

# Test 2: Create test users
echo -e "\n${GREEN}Test 2:${NC} Creating test users via API"
USER1_NICKNAME="testuser1_$(date +%s)"
USER2_NICKNAME="testuser2_$(date +%s)"

CREATE_USER1_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "{\"username\":\"$USER1_NICKNAME\"}" $USER_SERVICE_URL/users/create)
CREATE_USER2_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "{\"username\":\"$USER2_NICKNAME\"}" $USER_SERVICE_URL/users/create)

echo "User 1 created: $USER1_NICKNAME"
echo "Response: $CREATE_USER1_RESPONSE"
echo "User 2 created: $USER2_NICKNAME"
echo "Response: $CREATE_USER2_RESPONSE"

# Wait for user creation to propagate
echo "Waiting 3 seconds for user creation to propagate..."
sleep 3

# Test 3: Create a post for testing likes
echo -e "\n${GREEN}Test 3:${NC} Creating a post for testing likes"
METADATA="{\"authorUsername\":\"$USER1_NICKNAME\",\"type\":\"ORIGINAL\",\"text\":\"Test post for social interactions\"}"
echo "Using metadata: $METADATA"

POST_RESPONSE=$(curl -s -X POST \
  -F "metadata=$METADATA;type=application/json" \
  "$REPO_POST_URL$POSTS_PATH")

echo "Response: $POST_RESPONSE"

POST_ID=$(echo $POST_RESPONSE | grep -o '"postId":"[^"]*"' | cut -d'"' -f4)
if [ -n "$POST_ID" ]; then
  echo -e "${GREEN}✓ Post created with ID: $POST_ID${NC}"
else
  echo -e "${RED}✗ Failed to create post${NC}"
  POST_ID="00000000-0000-0000-0000-000000000000"
fi

# Test 4: User2 follows User1
echo -e "\n${GREEN}Test 4:${NC} Testing follow endpoint (User2 follows User1)"
FOLLOW_RESPONSE=$(curl -s -X POST "$REPO_SOCIAL_URL$SOCIAL_PATH/$USER2_NICKNAME/follow/$USER1_NICKNAME")
echo "Follow response: $FOLLOW_RESPONSE"

# Test 5: Check if User1 has followers
echo -e "\n${GREEN}Test 5:${NC} Checking followers of User1"
FOLLOWERS_RESPONSE=$(curl -s "$REPO_SOCIAL_URL$SOCIAL_PATH/users/$USER1_NICKNAME/followers")
echo "Followers response: $FOLLOWERS_RESPONSE"

# Check if the followers list contains User2
if echo $FOLLOWERS_RESPONSE | grep -q "$USER2_NICKNAME"; then
  echo -e "${GREEN}✓ User2 is following User1${NC}"
else
  echo -e "${RED}✗ User2 is not following User1${NC}"
fi

# Test 6: Check if User2 is following User1
echo -e "\n${GREEN}Test 6:${NC} Checking who User2 is following"
FOLLOWS_RESPONSE=$(curl -s "$REPO_SOCIAL_URL$SOCIAL_PATH/users/$USER2_NICKNAME/follows")
echo "Follows response: $FOLLOWS_RESPONSE"

# Test 7: User2 likes User1's post
echo -e "\n${GREEN}Test 7:${NC} User2 likes User1's post with ID $POST_ID"
LIKE_RESPONSE=$(curl -s -X POST "$REPO_SOCIAL_URL$SOCIAL_PATH/$USER2_NICKNAME/like/$POST_ID")
echo "Like response: $LIKE_RESPONSE"

# Test 8: Check users who liked the post
echo -e "\n${GREEN}Test 8:${NC} Checking users who liked the post"
LIKE_USERS_RESPONSE=$(curl -s "$REPO_SOCIAL_URL$SOCIAL_PATH/posts/$POST_ID/likeUsers")
echo "Like users response: $LIKE_USERS_RESPONSE"

# Check if the like users list contains User2
if echo $LIKE_USERS_RESPONSE | grep -q "$USER2_NICKNAME"; then
  echo -e "${GREEN}✓ User2 has liked the post${NC}"
else
  echo -e "${RED}✗ User2 has not liked the post${NC}"
fi

# Test 9: Check posts liked by User2
echo -e "\n${GREEN}Test 9:${NC} Checking posts liked by User2"
LIKED_POSTS_RESPONSE=$(curl -s "$REPO_SOCIAL_URL$SOCIAL_PATH/users/$USER2_NICKNAME/likedPosts")
echo "Liked posts response: $LIKED_POSTS_RESPONSE"

# Check if the liked posts list contains the created post
if echo $LIKED_POSTS_RESPONSE | grep -q "$POST_ID"; then
  echo -e "${GREEN}✓ Post is in User2's liked posts${NC}"
else
  echo -e "${RED}✗ Post is not in User2's liked posts${NC}"
fi

# Test 10: User2 unlikes the post
echo -e "\n${GREEN}Test 10:${NC} User2 unlikes the post"
UNLIKE_RESPONSE=$(curl -s -X POST "$REPO_SOCIAL_URL$SOCIAL_PATH/$USER2_NICKNAME/unlike/$POST_ID")
echo "Unlike response: $UNLIKE_RESPONSE"

# Test 11: Verify post was unliked
echo -e "\n${GREEN}Test 11:${NC} Verifying post was unliked"
LIKE_USERS_AFTER_UNLIKE=$(curl -s "$REPO_SOCIAL_URL$SOCIAL_PATH/posts/$POST_ID/likeUsers")
echo "Like users after unlike: $LIKE_USERS_AFTER_UNLIKE"

# Check if user2 is no longer in the likes list
if echo $LIKE_USERS_AFTER_UNLIKE | grep -q "$USER2_NICKNAME"; then
  echo -e "${RED}✗ User2 still likes the post${NC}"
else
  echo -e "${GREEN}✓ User2 has successfully unliked the post${NC}"
fi

# Test 12: User2 unfollows User1
echo -e "\n${GREEN}Test 12:${NC} User2 unfollows User1"
UNFOLLOW_RESPONSE=$(curl -s -X POST "$REPO_SOCIAL_URL$SOCIAL_PATH/$USER2_NICKNAME/unfollow/$USER1_NICKNAME")
echo "Unfollow response: $UNFOLLOW_RESPONSE"

# Test 13: Verify unfollow worked
echo -e "\n${GREEN}Test 13:${NC} Verifying unfollow worked"
FOLLOWERS_AFTER_UNFOLLOW=$(curl -s "$REPO_SOCIAL_URL$SOCIAL_PATH/users/$USER1_NICKNAME/followers")
echo "Followers after unfollow: $FOLLOWERS_AFTER_UNFOLLOW"

# Check if User2 is no longer following User1
if echo $FOLLOWERS_AFTER_UNFOLLOW | grep -q "$USER2_NICKNAME"; then
  echo -e "${RED}✗ User2 is still following User1${NC}"
else
  echo -e "${GREEN}✓ User2 has successfully unfollowed User1${NC}"
fi

# Test 14: User1 blocks User2
echo -e "\n${GREEN}Test 14:${NC} User1 blocks User2"
BLOCK_RESPONSE=$(curl -s -X POST "$REPO_SOCIAL_URL$SOCIAL_PATH/$USER1_NICKNAME/block/$USER2_NICKNAME")
echo "Block response: $BLOCK_RESPONSE"

# Test 15: Verify block worked by checking blocked users list
echo -e "\n${GREEN}Test 15:${NC} Verifying block worked"
BLOCKED_USERS=$(curl -s "$REPO_SOCIAL_URL$SOCIAL_PATH/users/$USER1_NICKNAME/blocked")
echo "Blocked users: $BLOCKED_USERS"

# Check if User2 is in User1's blocked list
if echo $BLOCKED_USERS | grep -q "$USER2_NICKNAME"; then
  echo -e "${GREEN}✓ User2 is in User1's blocked list${NC}"
else
  echo -e "${RED}✗ User2 is not in User1's blocked list${NC}"
fi

# Test 16: Verify User2 is being blocked
echo -e "\n${GREEN}Test 16:${NC} Verifying User2 is being blocked"
BLOCKING_USERS=$(curl -s "$REPO_SOCIAL_URL$SOCIAL_PATH/users/$USER2_NICKNAME/isblocked")
echo "Users blocking User2: $BLOCKING_USERS"

# Check if User1 is blocking User2
if echo $BLOCKING_USERS | grep -q "$USER1_NICKNAME"; then
  echo -e "${GREEN}✓ User1 is blocking User2${NC}"
else
  echo -e "${RED}✗ User1 is not blocking User2${NC}"
fi

# Test 17: Attempt to follow while blocked
echo -e "\n${GREEN}Test 17:${NC} User2 attempts to follow User1 while blocked"
FOLLOW_BLOCKED_RESPONSE=$(curl -s -X POST "$REPO_SOCIAL_URL$SOCIAL_PATH/$USER2_NICKNAME/follow/$USER1_NICKNAME")
echo "Follow while blocked response: $FOLLOW_BLOCKED_RESPONSE"

# Test 18: User1 unblocks User2
echo -e "\n${GREEN}Test 18:${NC} User1 unblocks User2"
UNBLOCK_RESPONSE=$(curl -s -X POST "$REPO_SOCIAL_URL$SOCIAL_PATH/$USER1_NICKNAME/unblock/$USER2_NICKNAME")
echo "Unblock response: $UNBLOCK_RESPONSE"

# Test 19: Verify unblock worked
echo -e "\n${GREEN}Test 19:${NC} Verifying unblock worked"
BLOCKED_AFTER_UNBLOCK=$(curl -s "$REPO_SOCIAL_URL$SOCIAL_PATH/users/$USER1_NICKNAME/blocked")
echo "Blocked users after unblock: $BLOCKED_AFTER_UNBLOCK"

# Check if User2 is no longer in User1's blocked list
if echo $BLOCKED_AFTER_UNBLOCK | grep -q "$USER2_NICKNAME"; then
  echo -e "${RED}✗ User2 is still in User1's blocked list${NC}"
else
  echo -e "${GREEN}✓ User2 has been successfully unblocked${NC}"
fi

# Test 20: Cleanup - delete the test post
echo -e "\n${GREEN}Test 20:${NC} Deleting test post"
DELETE_POST_RESPONSE=$(curl -s -X POST -H "X-user-name: $USER1_NICKNAME" "$REPO_POST_URL$POSTS_PATH/$POST_ID")
echo "Delete post response: $DELETE_POST_RESPONSE"

# Test 21: Cleanup - delete test users
echo -e "\n${GREEN}Test 21:${NC} Deleting test users"
DELETE_USER1_RESPONSE=$(curl -s -X DELETE "$USER_SERVICE_URL/users/delete/$USER1_NICKNAME")
DELETE_USER2_RESPONSE=$(curl -s -X DELETE "$USER_SERVICE_URL/users/delete/$USER2_NICKNAME")
echo "Delete User1 response: $DELETE_USER1_RESPONSE"
echo "Delete User2 response: $DELETE_USER2_RESPONSE"

echo -e "\n${GREEN}All tests completed${NC}"
