#!/bin/bash

# Set API endpoint
API_URL="http://192.168.122.104"

NICKNAME="testuser"

echo "Testing User API with nickname: $NICKNAME"
echo "-----------------------------------"

# Test creating a user
echo "1. Creating a user:"
CREATE_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "{\"username\":\"$NICKNAME\"}" $API_URL/users/create -w "\nStatus: %{http_code}")
echo "$CREATE_RESPONSE"
echo ""

echo "2. Getting user details by username:"
GET_RESPONSE=$(curl -s -X GET $API_URL/users/get/$NICKNAME -w "\nStatus: %{http_code}")
echo "$GET_RESPONSE"
echo ""

USER_ID=$(echo "$GET_RESPONSE" | grep -o '"id":"[^"]*' | sed 's/"id":"//')
if [ -n "$USER_ID" ]; then
    echo "Retrieved user ID: $USER_ID"
    
    echo "3. Getting user details by ID:"
    GET_ID_RESPONSE=$(curl -s -X GET $API_URL/users/get/$USER_ID -w "\nStatus: %{http_code}")
    echo "$GET_ID_RESPONSE"
    echo ""
else
    echo "Could not extract user ID from response."
fi

echo "4. Getting non-existent user:"
FAKE_UUID=$(cat /proc/sys/kernel/random/uuid)
GET_FAKE_RESPONSE=$(curl -s -X GET $API_URL/users/get/$FAKE_UUID -w "\nStatus: %{http_code}")
echo "$GET_FAKE_RESPONSE"
echo ""

# Test deleting a user by username
echo "5. Deleting user by username:"
DELETE_RESPONSE=$(curl -s -X DELETE $API_URL/users/delete/$NICKNAME -w "\nStatus: %{http_code}")
echo "$DELETE_RESPONSE"
echo ""

# Verify user is deleted
echo "6. Verifying user deletion:"
GET_DELETED_RESPONSE=$(curl -s -X GET $API_URL/users/get/$NICKNAME -w "\nStatus: %{http_code}")
echo "$GET_DELETED_RESPONSE"
echo ""

echo "API testing completed." 