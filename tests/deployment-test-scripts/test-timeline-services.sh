#!/bin/bash

# Set API endpoint
API_URL="http://192.168.122.104"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# Test users
USERNAME="timeline_test_user"
FOLLOWER_USERNAME="timeline_test_follower"

echo -e "${GREEN}=== Testing Timeline Services ===${NC}"
echo -e "${YELLOW}Using API URL: $API_URL${NC}"
echo -e "${YELLOW}Test users: $USERNAME and $FOLLOWER_USERNAME${NC}"
echo "----------------------------------------------"

# Step 1: Create test users
echo -e "${GREEN}1. Creating test users...${NC}"
CREATE_USER_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "{\"username\":\"$USERNAME\"}" $API_URL/users/create)
CREATE_FOLLOWER_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "{\"username\":\"$FOLLOWER_USERNAME\"}" $API_URL/users/create)
echo "Created user: $USERNAME"
echo "Created follower: $FOLLOWER_USERNAME"

# Step 2: Follower follows the main user
echo -e "\n${GREEN}2. Setting up follow relationship...${NC}"
FOLLOW_RESPONSE=$(curl -s -X POST $API_URL/social/$FOLLOWER_USERNAME/follow/$USERNAME)
echo "Follow response: $FOLLOW_RESPONSE"

# Step 3: Create some posts for the main user
echo -e "\n${GREEN}3. Creating test posts...${NC}"
for i in {1..3}; do
  POST_RESPONSE=$(curl -s -X POST \
    -H "Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW" \
    -F "metadata={\"authorUsername\":\"$USERNAME\",\"text\":\"Test post $i for timeline testing\",\"type\":\"ORIGINAL\"};type=application/json" \
    $API_URL/posts)
  
  echo "Created post $i: $POST_RESPONSE"
  
  # Extract post ID and store it
  POST_ID=$(echo $POST_RESPONSE | grep -o '"postId":"[^"]*"' | head -1 | cut -d'"' -f4)
  if [ -n "$POST_ID" ]; then
    eval "POST_ID_$i=$POST_ID"
    echo "Stored post ID $i: $POST_ID"
  fi
  
  # Small delay between posts
  sleep 1
done

# Step 4: Follower likes one of the posts
echo -e "\n${GREEN}4. Follower likes a post...${NC}"
LIKE_RESPONSE=$(curl -s -X POST $API_URL/social/$FOLLOWER_USERNAME/like/$POST_ID_2)
echo "Like response: $LIKE_RESPONSE"

# Step 5: Wait for timeline services to process the interactions
echo -e "\n${GREEN}5. Waiting for timeline services to process events...${NC}"
sleep 5

# Step 6: Test user timeline API
echo -e "\n${GREEN}6. Testing User Timeline API...${NC}"
USER_TIMELINE_RESPONSE=$(curl -s $API_URL/timelines/user/$USERNAME)
echo "User Timeline response for $USERNAME:"
echo "$USER_TIMELINE_RESPONSE" | json_pp 2>/dev/null || echo "$USER_TIMELINE_RESPONSE"

# Check if user timeline contains posts
TIMELINE_POST_COUNT=$(echo "$USER_TIMELINE_RESPONSE" | grep -o '"postId"' | wc -l)
if [ "$TIMELINE_POST_COUNT" -gt 0 ]; then
  echo -e "${GREEN}✓ User timeline API works - found $TIMELINE_POST_COUNT posts${NC}"
else
  echo -e "${RED}✗ User timeline API test failed - no posts found${NC}"
fi

# Step 7: Test home timeline API
echo -e "\n${GREEN}7. Testing Home Timeline API...${NC}"
HOME_TIMELINE_RESPONSE=$(curl -s $API_URL/timelines/home/$FOLLOWER_USERNAME)
echo "Home Timeline response for $FOLLOWER_USERNAME:"
echo "$HOME_TIMELINE_RESPONSE" | json_pp 2>/dev/null || echo "$HOME_TIMELINE_RESPONSE"

# Check if home timeline contains posts from followed user
HOME_TIMELINE_POST_COUNT=$(echo "$HOME_TIMELINE_RESPONSE" | grep -o '"postId"' | wc -l)
if [ "$HOME_TIMELINE_POST_COUNT" -gt 0 ]; then
  echo -e "${GREEN}✓ Home timeline API works - found $HOME_TIMELINE_POST_COUNT posts${NC}"
else
  echo -e "${RED}✗ Home timeline API test failed - no posts found${NC}"
fi

# Step 8: Clean up - delete test users and their posts
echo -e "\n${GREEN}8. Cleaning up test data...${NC}"
DELETE_USER_RESPONSE=$(curl -s -X DELETE $API_URL/users/delete/$USERNAME)
DELETE_FOLLOWER_RESPONSE=$(curl -s -X DELETE $API_URL/users/delete/$FOLLOWER_USERNAME)
echo "Deleted test users"

echo -e "\n${GREEN}Timeline services integration test completed!${NC}" 