package com.epita.service;

import com.epita.repository.entity.HomeTimelines;
import com.epita.repository.entity.TimelinePost;
import com.epita.repository.entity.UserActivity;
import com.epita.repository.entity.UserActivity.Posts;
import com.epita.repository.UserActivityRepository;
import com.epita.repository.HomeTimelineRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


import java.util.*;

@ApplicationScoped
public class HomeTimelineService {

    @Inject
    HomeTimelineRepository homeTimelineRepository;

    @Inject
    UserActivityRepository userActivityRepository;

    /**
     * Fetches the home timeline for a given user. If the timeline does not exist, it creates a new one.
     * 
     * @param username       the username of the user whose timeline is to be fetched
     */
    public HomeTimelines getTimeline(String username) {
        // Check if the timeline already exists
        HomeTimelines timeline = homeTimelineRepository.findTimeline(username);
        if (timeline != null && timeline.getPosts() != null) {
            return timeline;
        }
        // If not, create a new timeline
        timeline = new HomeTimelines();
        timeline.setUsername(username);
        timeline.setPosts(new ArrayList<>());
        timeline.setLastUpdated(new Date());
        homeTimelineRepository.persist(timeline);
        return timeline;
    }

    /**
     * Adds a post to the user's activity and updates the home timelines of their followers.
     * 
     * @param username   the username of the user who created the post
     * @param postId     the ID of the post
     * @param createdAt  the date when the post was created
     */
    public void addPost(String username, String postId, Date createdAt) {
        // Update UserActivity
        UserActivity userActivity = userActivityRepository.findActivity(username);
        if (userActivity == null) {
            userActivity = new UserActivity(username,
                new ArrayList<>(),
                new ArrayList<>()
            );
            userActivityRepository.persist(userActivity);
        }
        // Add post to UserActivity
        Posts newPost = new Posts();
        newPost.setPostId(postId);
        newPost.setType("POSTED");
        newPost.setDate(createdAt);
        userActivity.getPosts().add(newPost);
        userActivityRepository.update(userActivity);

        // Update HomeTimeline of followers
        List<String> followers = userActivityRepository.getFollowers(username);

        for (String follower : followers) {
            HomeTimelines timeline = getTimeline(follower);
            TimelinePost timelinePost = new TimelinePost(postId, username, "POSTED", createdAt);
            timeline.getPosts().add(timelinePost);
            timeline.getPosts().sort((p1, p2) -> p2.getDate().compareTo(p1.getDate()));
            timeline.setLastUpdated(new Date());
            homeTimelineRepository.update(timeline);
        }
    }

    /**
     * Adds a like to a post and updates the home timelines of the followers of the user who liked the post.
     * 
     * @param username   the username of the user who liked the post
     * @param postId     the ID of the post that was liked
     */
    public void addLike(String username, String postId) {
        Date likedAt = new Date();

        // Update UserActivity
        UserActivity userActivity = userActivityRepository.findActivity(username);
        if (userActivity == null) {
            userActivity = new UserActivity(username,
                new ArrayList<>(),
                new ArrayList<>()
            );
            userActivityRepository.persist(userActivity);
        }
        // Check if the post is already liked
        boolean alreadyLiked = userActivity.getPosts().stream()
                .anyMatch(post -> post.getPostId().equals(postId) && post.getType().equals("LIKED"));
        if (alreadyLiked) {
            return;
        }

        // Add like to UserActivity
        Posts likedPost = new Posts();
        likedPost.setPostId(postId);
        likedPost.setType("LIKED");
        likedPost.setDate(likedAt);
        userActivity.getPosts().add(likedPost);
        userActivityRepository.update(userActivity);

        // Update HomeTimeline of followers
        List<String> followers = userActivityRepository.getFollowers(username);

        for (String follower : followers) {
            HomeTimelines timeline = getTimeline(follower);
            TimelinePost timelinePost = new TimelinePost(postId, username, "LIKED", likedAt);
            timeline.getPosts().add(timelinePost);
            timeline.getPosts().sort((p1, p2) -> p2.getDate().compareTo(p1.getDate()));
            timeline.setLastUpdated(new Date());
            homeTimelineRepository.update(timeline);
        }
    }

    /**
     * Removes a post from the user's activity and updates the home timelines of all users.
     * 
     * @param username   the username of the user who created the post
     * @param postId     the ID of the post to be removed
     */
    public void removePost(String username, String postId) {
        // Update UserActivity
        UserActivity userActivity = userActivityRepository.findActivity(username);
        if (userActivity != null && userActivity.getPosts() != null) {
            userActivity.getPosts().removeIf(post -> post.getPostId().equals(postId) && post.getType().equals("POSTED"));
            userActivityRepository.update(userActivity);
        }

        // Update HomeTimeline of all users
        List<HomeTimelines> timelines = homeTimelineRepository.listAll();
        for (HomeTimelines timeline : timelines) {
            if (timeline.getPosts().removeIf(post -> post.getPostId().equals(postId) && post.getAuthorId().equals(username) && post.getType().equals("POSTED"))) {
                timeline.getPosts().sort((p1, p2) -> p2.getDate().compareTo(p1.getDate()));
                timeline.setLastUpdated(new Date());
                homeTimelineRepository.update(timeline);
            }
        }
    }

    /**
     * Removes a like from a post and updates the home timelines of the followers of the user who unliked the post.
     * 
     * @param username   the username of the user who unliked the post
     * @param postId     the ID of the post that was unliked
     */
    public void removeLike(String username, String postId) {
        UserActivity userActivity = userActivityRepository.findActivity(username);
        if (userActivity == null) {
            userActivity = new UserActivity(username,
                new ArrayList<>(),
                new ArrayList<>()
            );
            userActivityRepository.persist(userActivity);
        }
        //remove like from useractivity
        boolean alreadyLiked = userActivity.getPosts().stream()
                .anyMatch(post -> post.getPostId().equals(postId) && post.getType().equals("LIKED"));

        if (!alreadyLiked) {
            return;
        }
        // Check if the post is already liked
        userActivity.getPosts().removeIf(post -> post.getPostId().equals(postId) && post.getType().equals("LIKED"));
        userActivityRepository.update(userActivity);

        // Update HomeTimeline of followers
        List<String> followers = userActivityRepository.getFollowers(username);

        for (String follower : followers) {
            HomeTimelines timeline = getTimeline(follower);
            boolean changed = timeline.getPosts().removeIf(p -> p.getPostId().equals(postId) && p.getAuthorId().equals(username) && p.getType().equals("LIKED"));
            if (changed) {
                timeline.getPosts().sort((p1, p2) -> p2.getDate().compareTo(p1.getDate()));
                timeline.setLastUpdated(new Date());
                homeTimelineRepository.update(timeline);
            }
        }
    }

    /**
     * Adds a follow relationship between two users and updates the home timeline of the follower.
     * 
     * @param username   the username of the user who is following
     * @param followed   the username of the user being followed
     */
    public void addFollow(String username, String followed) {
        UserActivity userActivity = userActivityRepository.findActivity(username);
        if (userActivity == null) {
            userActivity = new UserActivity(username,
                new ArrayList<>(),
                new ArrayList<>()
            );
            userActivityRepository.persist(userActivity);
        }

        if (!userActivity.getFollowed().contains(followed)) {
            userActivity.getFollowed().add(followed);
            userActivityRepository.update(userActivity);

            // Fetch posts from the followed user
            UserActivity followedUserActivity = userActivityRepository.findActivity(followed);
            if (followedUserActivity != null && followedUserActivity.getPosts() != null) {
                HomeTimelines timeline = getTimeline(username);

                for (UserActivity.Posts post : followedUserActivity.getPosts()) {
                    TimelinePost timelinePost = new TimelinePost(
                        post.getPostId(),
                        followed,
                        post.getType(),
                        post.getDate()
                    );
                    timeline.getPosts().add(timelinePost);
                }

                // Sort posts by date in descending order
                timeline.getPosts().sort((p1, p2) -> p2.getDate().compareTo(p1.getDate()));
                timeline.setLastUpdated(new Date());
                homeTimelineRepository.update(timeline);
            }
        }
    }

    /**
     * Removes a follow relationship between two users and updates the home timeline of the unfollower.
     * 
     * @param username   the username of the user who is unfollowing
     * @param unfollowed the username of the user being unfollowed
     */
    public void removeFollow(String username, String unfollowed) {
        // Update UserActivity
        UserActivity userActivity = userActivityRepository.findActivity(username);
        if (userActivity != null && userActivity.getFollowed() != null) {
            userActivity.getFollowed().remove(unfollowed);
            userActivityRepository.update(userActivity);
        }

        // Remove posts from the unfollowed user in the timeline
        HomeTimelines timeline = getTimeline(username);
        if (timeline != null && timeline.getPosts() != null) {
            boolean changed = timeline.getPosts().removeIf(post -> post.getAuthorId().equals(unfollowed));
            if (changed) {
                timeline.setLastUpdated(new Date());
                homeTimelineRepository.update(timeline);
            }
        }
    }
}
