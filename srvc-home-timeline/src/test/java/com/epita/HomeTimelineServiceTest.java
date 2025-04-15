package com.epita;

import com.epita.repository.entity.HomeTimelines;
import com.epita.events.UserActionEvent;
import com.epita.redis.UserEventPublisherTest;
import com.epita.service.HomeTimelineService;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class HomeTimelineServiceTest {

    @Inject
    HomeTimelineService homeTimelineService;

    @Inject
    UserEventPublisherTest userEventPublisherTest;

    Date date1 = new Date("December 17, 1995 03:24:00");
    Date date2 = new Date("January 20, 2015 17:10:52");
    Date date3 = new Date("February 28, 2020 12:00:00");

    @BeforeEach
    public void resetEverythingBefore() {
        // unfollow everyone and delete every posts
        UserActionEvent event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.USER_UNFOLLOWED);
        event.setUsername("user1");
        event.setTargetUsername("user2");
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.USER_UNFOLLOWED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event.setUsername("user2");
        event.setTargetUsername("user1");
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.USER_UNFOLLOWED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event.setUsername("user3");
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.USER_UNFOLLOWED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event.setActionType(UserActionEvent.ActionType.POST_DELETED);
        event.setUsername("user1");
        event.setPostId("post1");
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_DELETED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event.setPostId("post2");
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_DELETED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event.setUsername("user2");
        event.setPostId("post3");
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_DELETED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event.setUsername("user1");
        event.setActionType(UserActionEvent.ActionType.POST_UNLIKED);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_UNLIKED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetTimeline() {
        String username1 = "user1";

        HomeTimelines timeline = homeTimelineService.getTimeline(username1);
        assertNotNull(timeline);
        assertEquals(username1, timeline.getUsername());
        assertEquals(0, timeline.getPosts().size());
    }

    @Test
    public void testCreatePost() {
        String username1 = "user1";
        String postId1 = "post1";

        UserActionEvent event = new UserActionEvent();
        event.setUsername(username1);
        event.setPostId(postId1);
        event.setActionType(UserActionEvent.ActionType.POST_CREATED);
        event.setPostContent("Hello, world!");
        event.setTimestamp(date1);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_CREATED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        HomeTimelines timeline = homeTimelineService.getTimeline(username1);
        assertNotNull(timeline);
        assertEquals(0, timeline.getPosts().size());
    }

    @Test
    public void testFollow() {
        String username1 = "user1";
        String username2 = "user2";
        String postId1 = "post1";

        UserActionEvent event = new UserActionEvent();
        event.setUsername(username1);
        event.setPostId(postId1);
        event.setActionType(UserActionEvent.ActionType.POST_CREATED);
        event.setPostContent("Hello, world!");
        event.setTimestamp(date1);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_CREATED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.USER_FOLLOWED);
        event.setUsername(username2);
        event.setTargetUsername(username1);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.USER_FOLLOWED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        HomeTimelines timeline = homeTimelineService.getTimeline(username2);
        assertNotNull(timeline);
        assertEquals(1, timeline.getPosts().size());
        assertEquals(postId1, timeline.getPosts().get(0).getPostId());
        assertEquals(date1, timeline.getPosts().get(0).getDate());
    }

    @Test
    public void testCreatePostAfterFollow() {
        String username1 = "user1";
        String username2 = "user2";
        String postId1 = "post1";
        String postId2 = "post2";

        UserActionEvent event = new UserActionEvent();
        event.setUsername(username1);
        event.setPostId(postId1);
        event.setActionType(UserActionEvent.ActionType.POST_CREATED);
        event.setPostContent("Hello, world!");
        event.setTimestamp(date1);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_CREATED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.USER_FOLLOWED);
        event.setUsername(username2);
        event.setTargetUsername(username1);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.USER_FOLLOWED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event = new UserActionEvent();
        event.setUsername(username1);
        event.setPostId(postId2);
        event.setActionType(UserActionEvent.ActionType.POST_CREATED);
        event.setPostContent("New post after follow");
        event.setTimestamp(date2);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_CREATED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        HomeTimelines timeline = homeTimelineService.getTimeline(username2);
        assertNotNull(timeline);
        assertEquals(2, timeline.getPosts().size());
        assertEquals("post2", timeline.getPosts().get(0).getPostId());
        assertEquals(date2, timeline.getPosts().get(0).getDate());
        assertEquals("post1", timeline.getPosts().get(1).getPostId());
        assertEquals(date1, timeline.getPosts().get(1).getDate());
    }

    @Test
    public void testUnfollow() {
        String username1 = "user1";
        String username2 = "user2";

        UserActionEvent event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.USER_FOLLOWED);
        event.setUsername(username2);
        event.setTargetUsername(username1);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.USER_FOLLOWED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.USER_UNFOLLOWED);
        event.setUsername(username2);
        event.setTargetUsername(username1);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.USER_UNFOLLOWED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        HomeTimelines timeline = homeTimelineService.getTimeline(username2);
        assertNotNull(timeline);
        assertEquals(0, timeline.getPosts().size());
    }

    @Test
    public void testUnfollowUnknown() {
        String username1 = "user1";
        String username2 = "user2";

        HomeTimelines timeline = homeTimelineService.getTimeline(username2);
        Date date = timeline.getLastUpdated();

        UserActionEvent event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.USER_UNFOLLOWED);
        event.setUsername(username2);
        event.setTargetUsername(username1);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.USER_UNFOLLOWED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        timeline = homeTimelineService.getTimeline(username2);
        assertNotNull(timeline);
        assertEquals(0, timeline.getPosts().size());
        assertEquals(date, timeline.getLastUpdated());
    }

    @Test
    public void testCrossFollow() {
        String username1 = "user1";
        String username2 = "user2";
        String postId1 = "post1";
        String postId2 = "post2";
        String postId3 = "post3";

        UserActionEvent event = new UserActionEvent();
        event.setUsername(username1);
        event.setPostId(postId1);
        event.setActionType(UserActionEvent.ActionType.POST_CREATED);
        event.setPostContent("Hello, world!");
        event.setTimestamp(date1);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_CREATED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event = new UserActionEvent();
        event.setUsername(username1);
        event.setPostId(postId2);
        event.setActionType(UserActionEvent.ActionType.POST_CREATED);
        event.setPostContent("Hello, world!");
        event.setTimestamp(date2);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_CREATED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.USER_FOLLOWED);
        event.setUsername(username1);
        event.setTargetUsername(username2);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.USER_FOLLOWED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.USER_FOLLOWED);
        event.setUsername(username2);
        event.setTargetUsername(username1);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.USER_FOLLOWED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event = new UserActionEvent();
        event.setUsername(username2);
        event.setPostId(postId3);
        event.setActionType(UserActionEvent.ActionType.POST_CREATED);
        event.setPostContent("Hello, world!");
        event.setTimestamp(date3);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_CREATED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        HomeTimelines timeline1 = homeTimelineService.getTimeline(username1);
        HomeTimelines timeline2 = homeTimelineService.getTimeline(username2);
        assertNotNull(timeline1);
        assertNotNull(timeline2);
        assertEquals(1, timeline1.getPosts().size());
        assertEquals(2, timeline2.getPosts().size());
        assertEquals(postId3, timeline1.getPosts().get(0).getPostId());
        assertEquals(date3, timeline1.getPosts().get(0).getDate());
    }

    @Test
    public void testDeletePost() {
        String username1 = "user1";
        String username2 = "user2";
        String postId1 = "post1";

        UserActionEvent event = new UserActionEvent();
        event.setUsername(username1);
        event.setPostId(postId1);
        event.setActionType(UserActionEvent.ActionType.POST_CREATED);
        event.setPostContent("Hello, world!");
        event.setTimestamp(date1);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_CREATED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.USER_FOLLOWED);
        event.setUsername(username2);
        event.setTargetUsername(username1);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.USER_FOLLOWED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        HomeTimelines timeline = homeTimelineService.getTimeline(username2);
        assertNotNull(timeline);
        assertEquals(1, timeline.getPosts().size());
        assertEquals(postId1, timeline.getPosts().get(0).getPostId());
        assertEquals(date1, timeline.getPosts().get(0).getDate());

        event = new UserActionEvent();
        event.setUsername(username1);
        event.setPostId(postId1);
        event.setActionType(UserActionEvent.ActionType.POST_DELETED);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_DELETED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        timeline = homeTimelineService.getTimeline(username2);
        assertNotNull(timeline);
        assertEquals(0, timeline.getPosts().size());
    }

    @Test
    public void testFollowAndLike() {
        String username1 = "user1";
        String username2 = "user2";
        String username3 = "user3";
        String postId1 = "post1";
        String postId3 = "post3";

        UserActionEvent event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.USER_FOLLOWED);
        event.setUsername(username3);
        event.setTargetUsername(username1);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.USER_FOLLOWED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event = new UserActionEvent();
        event.setUsername(username1);
        event.setTargetUsername(username2);
        event.setActionType(UserActionEvent.ActionType.USER_FOLLOWED);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.USER_FOLLOWED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event = new UserActionEvent();
        event.setUsername(username2);
        event.setPostId(postId3);
        event.setActionType(UserActionEvent.ActionType.POST_CREATED);
        event.setPostContent("Hello, world!");
        event.setTimestamp(date3);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_CREATED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event = new UserActionEvent();
        event.setUsername(username1);
        event.setPostId(postId1);
        event.setActionType(UserActionEvent.ActionType.POST_CREATED);
        event.setPostContent("Hello, world!");
        event.setTimestamp(date1);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_CREATED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        HomeTimelines timeline = homeTimelineService.getTimeline(username3);
        assertNotNull(timeline);
        assertEquals(1, timeline.getPosts().size());
        assertEquals(postId1, timeline.getPosts().get(0).getPostId());
        assertEquals("POSTED", timeline.getPosts().get(0).getType());
        assertEquals(username1, timeline.getPosts().get(0).getAuthorId());

        event = new UserActionEvent();
        event.setUsername(username1);
        event.setPostId(postId3);
        event.setActionType(UserActionEvent.ActionType.POST_LIKED);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_LIKED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        timeline = homeTimelineService.getTimeline(username3);
        assertNotNull(timeline);
        assertEquals(2, timeline.getPosts().size());
        assertEquals(postId3, timeline.getPosts().get(0).getPostId());
        assertEquals("LIKED", timeline.getPosts().get(0).getType());
        assertEquals(username1, timeline.getPosts().get(0).getAuthorId());
    }

    @Test
    public void testUnlike() {
        String username1 = "user1";
        String username2 = "user2";
        String username3 = "user3";
        String postId3 = "post3";

        UserActionEvent event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.USER_FOLLOWED);
        event.setUsername(username3);
        event.setTargetUsername(username1);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.USER_FOLLOWED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event = new UserActionEvent();
        event.setUsername(username1);
        event.setTargetUsername(username2);
        event.setActionType(UserActionEvent.ActionType.USER_FOLLOWED);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.USER_FOLLOWED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event = new UserActionEvent();
        event.setUsername(username2);
        event.setPostId(postId3);
        event.setActionType(UserActionEvent.ActionType.POST_CREATED);
        event.setPostContent("Hello, world!");
        event.setTimestamp(date3);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_CREATED.getValue());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        event = new UserActionEvent();
        event.setUsername(username1);
        event.setPostId(postId3);
        event.setActionType(UserActionEvent.ActionType.POST_LIKED);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_LIKED.getValue());

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        HomeTimelines timeline = homeTimelineService.getTimeline(username3);
        assertNotNull(timeline);
        assertEquals(1, timeline.getPosts().size());
        assertEquals(postId3, timeline.getPosts().get(0).getPostId());
        assertEquals("LIKED", timeline.getPosts().get(0).getType());

        timeline = homeTimelineService.getTimeline(username1);
        assertNotNull(timeline);
        assertEquals(1, timeline.getPosts().size());
        assertEquals(postId3, timeline.getPosts().get(0).getPostId());

        event = new UserActionEvent();
        event.setUsername(username1);
        event.setPostId(postId3);
        event.setActionType(UserActionEvent.ActionType.POST_UNLIKED);
        userEventPublisherTest.publishAction(event, UserActionEvent.ActionType.POST_UNLIKED.getValue());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        timeline = homeTimelineService.getTimeline(username3);
        assertNotNull(timeline);
        assertEquals(0, timeline.getPosts().size());
        timeline = homeTimelineService.getTimeline(username1);
        assertNotNull(timeline);
        assertEquals(1, timeline.getPosts().size());
        assertEquals(postId3, timeline.getPosts().get(0).getPostId());
        assertEquals("POSTED", timeline.getPosts().get(0).getType());
    }
}
