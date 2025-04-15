package com.epita;

import com.epita.events.UserActionEvent;
import com.epita.redis.UserTimelineActionSubscriber;
import com.epita.repository.ServiceUserTimelineMongoRepo;
import com.epita.repository.models.UserTimelineModel;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ServiceUserTimelineControllerTest {
    
    @Inject
    ServiceUserTimelineMongoRepo repo;
    
    @Inject
    UserTimelineActionSubscriber userTimelineActionSubscriber;
    
    private static final String EXISTING_USERNAME = "testTimelineUser";
    private static final String NON_EXISTENT_USERNAME = "nonExistentUser";
    private static final String EXISTING_USER_ID = UUID.randomUUID().toString();
    private static final String POST_ID = UUID.randomUUID().toString();
    
    @BeforeEach
    void setup() {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        
        // Clear any existing timeline data
        repo.deleteUserTmelineWithUsername(EXISTING_USERNAME);
        repo.deleteUserTmelineWithUsername(NON_EXISTENT_USERNAME);
        
        // Create a test user with timeline data
        UserActionEvent userEvent = new UserActionEvent();
        userEvent.setActionType(UserActionEvent.ActionType.USER_CREATED);
        userEvent.setUserId(EXISTING_USER_ID);
        userEvent.setUsername(EXISTING_USERNAME);
        userEvent.setTimestamp(Date.from(Instant.now()));
        
        // Create the user timeline
        userTimelineActionSubscriber.accept(userEvent);
        
        // Wait for user timeline creation (optional: add helper method for waiting)
        waitForUserTimelineCreation(EXISTING_USERNAME);
        
        // Add a post to the timeline
        UserActionEvent postEvent = new UserActionEvent();
        postEvent.setActionType(UserActionEvent.ActionType.POST_CREATED);
        postEvent.setUserId(EXISTING_USER_ID);
        postEvent.setUsername(EXISTING_USERNAME);
        postEvent.setPostId(POST_ID);
        postEvent.setTimestamp(Date.from(Instant.now()));
        
        // Add the post to the timeline
        userTimelineActionSubscriber.accept(postEvent);
        
        // Wait for post to be added to timeline
        waitForPostInTimeline(EXISTING_USERNAME, POST_ID);
    }
    
    // Helper method to wait for timeline creation
    private void waitForUserTimelineCreation(String username) {
        for (int i = 0; i < 10; i++) {
            try {
                UserTimelineModel timeline = repo.getUserTimelineWithUsername(username);
                if (timeline != null) {
                    return;
                }
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // Helper method to wait for post to be added to timeline
    private void waitForPostInTimeline(String username, String postId) {
        for (int i = 0; i < 10; i++) {
            try {
                UserTimelineModel timeline = repo.getUserTimelineWithUsername(username);
                if (timeline != null && timeline.getPosts().stream().anyMatch(post -> 
                        post.getPostId().equals(postId))) {
                    return;
                }
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    void testGetUserTimeline() {
        // Test getting timeline for an existing user
        given()
            .when().get("/timelines/user/" + EXISTING_USERNAME)
            .then()
            .statusCode(200)
            .body("id", notNullValue());
    }

    @Test
    void testGetNonExistentUserTimeline() {
        // Test getting timeline for a non-existent user
        given()
            .when().get("/timelines/user/" + NON_EXISTENT_USERNAME)
            .then()
            .statusCode(404);
    }

    @Test
    void testGetTimelineWithEmptyUsername() {
        // Test getting timeline with an empty username
        given()
            .when().get("/timelines/user/")
            .then()
            .statusCode(404);
            
        // Test with empty string in the path
        given()
            .when().get("/timelines/user/ ")
            .then()
            .statusCode(404);
    }
    
    @Test
    void testGetTimelineWithNullUsername() {
        given()
            .when().get("/timelines/user/\"\"")
            .then()
            .statusCode(404);
    }
}
