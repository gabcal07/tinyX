package com.epita;

import com.epita.events.UserActionEvent;
import com.epita.redis.UserEventsPublisher;
import com.epita.redis.PostEventsPublisher;
import com.epita.redis.UserTimelineActionSubscriber;
import com.epita.repository.ServiceUserTimelineMongoRepo;
import com.epita.repository.models.UserTimelineModel;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.io.Console;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserTimelineRedisIntegrationTest {

    @Inject
    UserEventsPublisher userEventsPublisher;

    @Inject
    PostEventsPublisher postEventsPublisher;

    @Inject
    ServiceUserTimelineMongoRepo repo;

    @Inject
    UserTimelineActionSubscriber userTimelineActionSubscriber;

    static final String USER_ID = "09f31175-56a1-481b-bb4d-bf5e897b28e7";
    static final String USERNAME = "redis-user";
    static final String POST_ID = "redis-post";

    /*@BeforeEach
    void clearDB() {
        repo.deleteUserTmelineWithUsername(USERNAME);
    }
*/
    @Test
    @Order(1)
    void shouldCreateTimelineWhenUserCreated() {
        repo.clearUserTimelines();
        // Create user event using publisher
        UserActionEvent event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.USER_CREATED);
        event.setUserId(USER_ID);
        event.setUsername(USERNAME);
        event.setTimestamp(Date.from(Instant.now()));

        // Publish the event
        // userEventsPublisher.publishUserCreated(event);
        userTimelineActionSubscriber.accept(event);

        // Wait and verify the timeline was created
        boolean timelineCreated = false;
        for (int i = 0; i < 10; i++) {
            try {

                // Check if timeline was created
                UserTimelineModel timeline = repo.getUserTimeline(UUID.fromString(USER_ID));
                if (timeline != null && timeline.getPosts().isEmpty()) {
                    timelineCreated = true;
                    break;
                }
                // Wait 500ms before checking again
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        assertTrue(timelineCreated, "Timeline should be created after user event");
    }

    @Test
    @Order(2)
    void shouldUpdateTimelineWhenPostCreated() {
        // Ensure user timeline exists first
        UserActionEvent userEvent = new UserActionEvent();
        userEvent.setActionType(UserActionEvent.ActionType.USER_CREATED);
        userEvent.setUserId(USER_ID);
        userEvent.setUsername(USERNAME);
        userEvent.setTimestamp(Date.from(Instant.now()));
        // userEventsPublisher.publishUserCreated(userEvent);
        userTimelineActionSubscriber.accept(userEvent);

        // Wait for user timeline creation
        waitForCondition(() -> repo.getUserTimelineWithUsername(USERNAME) != null);

        // Create post event
        UserActionEvent postEvent = new UserActionEvent();
        postEvent.setActionType(UserActionEvent.ActionType.POST_CREATED);
        postEvent.setUserId(USER_ID);
        postEvent.setUsername(USERNAME);
        postEvent.setPostId(POST_ID);
        postEvent.setTimestamp(Date.from(Instant.now()));

        // Publish the post created event
        userTimelineActionSubscriber.accept(postEvent);


        // Wait and verify the post was added to timeline
        boolean postAdded = false;
        for (int i = 0; i < 10; i++) {
            try {
                UserTimelineModel timeline = repo.getUserTimelineWithUsername(USERNAME);
                if (timeline != null && timeline.getPosts().stream().anyMatch(post ->
                        post.getPostId().equals(POST_ID) && post.getType().equals("AUTHORED"))) {
                    postAdded = true;
                    break;
                }
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        assertTrue(postAdded, "Timeline should contain the post ID after post event");

        UserActionEvent deleteEvent = new UserActionEvent();
        deleteEvent.setActionType(UserActionEvent.ActionType.POST_DELETED);
        deleteEvent.setUserId(USER_ID);
        deleteEvent.setUsername(USERNAME);
        deleteEvent.setPostId(POST_ID);

        // Publish the post created event
        userTimelineActionSubscriber.accept(deleteEvent);

        for (int i = 0; i < 10; i++) {
            try {
                UserTimelineModel timeline = repo.getUserTimelineWithUsername(USERNAME);
                if (timeline != null && !timeline.getPosts().stream().anyMatch(post ->
                        post.getPostId().equals(POST_ID))) {
                    postAdded = false;
                    break;
                }
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        assertFalse(postAdded, "Timeline shouldnt not contain the post ID after post event");
    }




    @Test
    @Order(3)
    void shouldUpdateTimelineWhenPostLiked() {
        // Ensure user timeline exists first
        UserActionEvent userEvent = new UserActionEvent();
        userEvent.setActionType(UserActionEvent.ActionType.USER_CREATED);
        userEvent.setUserId(USER_ID);
        userEvent.setUsername(USERNAME);
        userEvent.setTimestamp(Date.from(Instant.now()));
        // userEventsPublisher.publishUserCreated(userEvent);
        userTimelineActionSubscriber.accept(userEvent);

        // Wait for user timeline creation
        waitForCondition(() -> repo.getUserTimelineWithUsername(USERNAME) != null);

        // Create post event
        UserActionEvent postEvent = new UserActionEvent();
        postEvent.setActionType(UserActionEvent.ActionType.POST_LIKED);
        postEvent.setUserId(USER_ID);
        postEvent.setUsername(USERNAME);
        postEvent.setPostId(POST_ID);
        postEvent.setTimestamp(Date.from(Instant.now()));

        // Publish the post created event
        userTimelineActionSubscriber.accept(postEvent);


        // Wait and verify the post was added to timeline
        boolean postAdded = false;
        for (int i = 0; i < 10; i++) {
            try {
                UserTimelineModel timeline = repo.getUserTimelineWithUsername(USERNAME);
                if (timeline != null && timeline.getPosts().stream().anyMatch(post ->
                        post.getPostId().equals(POST_ID) && post.getType().equals("LIKED"))) {
                    postAdded = true;
                    break;
                }
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        assertTrue(postAdded, "Timeline should contain the post ID after post event");

        UserActionEvent deleteEvent = new UserActionEvent();
        deleteEvent.setActionType(UserActionEvent.ActionType.POST_UNLIKED);
        deleteEvent.setUserId(USER_ID);
        deleteEvent.setUsername(USERNAME);
        deleteEvent.setPostId(POST_ID);

        // Publish the post created event
        userTimelineActionSubscriber.accept(deleteEvent);

        for (int i = 0; i < 10; i++) {
            try {
                UserTimelineModel timeline = repo.getUserTimelineWithUsername(USERNAME);
                if (timeline != null && !timeline.getPosts().stream().anyMatch(post ->
                        post.getPostId().equals(POST_ID))) {
                    postAdded = false;
                    break;
                }
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        assertFalse(postAdded, "Timeline shouldnt not contain the post ID after post event");

        // Ensure user timeline exists first
        UserActionEvent userDeleteEvent = new UserActionEvent();
        userDeleteEvent.setActionType(UserActionEvent.ActionType.USER_DELETED);
        userDeleteEvent.setUserId(USER_ID);
        userDeleteEvent.setUsername(USERNAME);
        // userEventsPublisher.publishUserCreated(userEvent);
        userTimelineActionSubscriber.accept(userDeleteEvent);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        boolean userExist = true;
        UserTimelineModel model = repo.getUserTimelineWithUsername(USERNAME);
        if (model == null)
        {
            userExist = false;
        }

        assertFalse(userExist, "Timeline should not exist");

    }

    private void waitForCondition(Condition condition) {
        for (int i = 0; i < 10; i++) {
            try {
                if (condition.check()) {
                    return;
                }
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @FunctionalInterface
    private interface Condition {
        boolean check();
    }
}

//@Inject
//    RedisClient redis;
//
//    @Inject
//    ServiceUserTimelineMongoRepo repo;
//
//    static final String USER_ID = "redis-user";
//    static final String POST_ID = "redis-post";
//
//    @BeforeEach
//    void clearDB() {
//        repo.deleteUserTmelineWithUsername(USER_ID);
//    }
//
//    @Test
//    @Order(1)
//    void shouldCreateTimelineWhenUserCreated() {
//        // Simule l'envoi d'un message Redis pour création d'user
//        String event = String.format("{\"eventType\": \"USER_CREATED\", \"userId\": \"%s\"}", USER_ID);
//        redis.publish("user-events", event.getBytes(StandardCharsets.UTF_8).toString());
//
//        // Attente manuelle avec Thread.sleep et vérification de la timeline
//        boolean timelineCreated = false;
//        for (int i = 0; i < 10; i++) {
//            try {
//                // Vérifier si la timeline a été créée
//                var timeline = repo.getUserTimelineWithUsername(USER_ID);
//                if (timeline != null && timeline.getPosts().isEmpty()) {
//                    timelineCreated = true;
//                    break;
//                }
//                // Attendre 500ms avant de re-vérifier
//                Thread.sleep(500);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//
//        assertTrue(timelineCreated, "Timeline should be created after user event");
//    }
//
//    @Test
//    @Order(2)
//    void shouldUpdateTimelineWhenPostCreated() {
//        // On suppose que la timeline existe déjà
//        //repo.save(new UserTimelineModel(USER_ID, List.of()));
//
//        // Simule la création d’un post
//        String event = String.format("{\"eventType\": \"POST_CREATED\", \"userId\": \"%s\", \"postId\": \"%s\"}", USER_ID, POST_ID);
//        redis.publish("post-events", event.getBytes(StandardCharsets.UTF_8).toString());
//
//        // Attente manuelle avec Thread.sleep et vérification de la timeline
//        boolean postAdded = false;
//        for (int i = 0; i < 10; i++) {
//            try {
//                // Vérifier si le post a été ajouté à la timeline
//                var updated = repo.getUserTimelineWithUsername(USER_ID);
//                if (updated != null && updated.getPosts().contains(POST_ID)) {
//                    postAdded = true;
//                    break;
//                }
//                // Attendre 500ms avant de re-vérifier
//                Thread.sleep(500);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//
//        assertTrue(postAdded, "Timeline should contain the post ID after post event");
//    }
