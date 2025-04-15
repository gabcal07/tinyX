package com.epita.service;

import com.epita.events.UserActionEvent;
import com.epita.redis.SocialActionPublisher;
import com.epita.repository.SocialRepoNeo4j;
import com.epita.repository.models.Node;
import com.epita.repository.models.Relationship;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.Logger;

import java.util.List;

@Slf4j
@ApplicationScoped
public class SocialService {

    @Inject
    SocialRepoNeo4j neo4jRepo;

    @Inject
    Logger logger;

    @Inject
    SocialActionPublisher publisher;

    /**
     * Handles the blocking of a user in Neo4j.
     * Deletes following relationships if they exist.
     * @param username       the nodeId of the user who blocked
     * @param targetUsername the nodeId of the user being blocked
     */
    public void userBlock(String username, String targetUsername) {
        // Créer la relation de blocage
        Node userNode = new Node(Node.NodeType.USER, username);
        Node targetUserNode = new Node(Node.NodeType.USER, targetUsername);
        Relationship relationship = new Relationship(userNode, targetUserNode, Relationship.RelationshipType.BLOCKS, System.currentTimeMillis());
        neo4jRepo.createOrUpdateRelationship(relationship);
        logger.info("[USER BLOCK][SERVICE]: Creating relationship: " + relationship);

        // Supprimer toute relation de suivi existante dans les deux sens
        boolean success = neo4jRepo.removeFollowRelationsBetweenUsers(username, targetUsername);
        if (success) {
            logger.info("[BLOCK USER][SERVICE]: Suppression des relations de suivi entre " + username + " et " + targetUsername);
            // Envoyer un événement de blocage
            UserActionEvent event = new UserActionEvent();
            event.setActionType(UserActionEvent.ActionType.USER_BLOCKED);
            event.setUsername(username);
            event.setTargetUsername(targetUsername);
            publisher.publishAction(event, UserActionEvent.ActionType.USER_BLOCKED.getValue());
        } else {
            logger.warn("[BLOCK USER][SERVICE]: Échec de la suppression des relations de suivi entre " + username + " et " + targetUsername);
        }
    }

    /**
     * Handles the unblocking of a user in Neo4j.
     *
     * @param username       the nodeId of the user who unblocked
     * @param targetUsername the nodeId of the user being unblocked
     */
    public void userUnblock(String username, String targetUsername) {
        Node userNode = new Node(Node.NodeType.USER, username);
        Node targetUserNode = new Node(Node.NodeType.USER, targetUsername);
        Relationship relationship = new Relationship(userNode, targetUserNode, Relationship.RelationshipType.BLOCKS, System.currentTimeMillis());
        logger.info("[USER BLOCK][SERVICE]: Deleting relationship: " + relationship);
        boolean success = neo4jRepo.deleteRelationship(relationship);
        if (!success) {
            logger.errorf("[CREATE USER][SERVICE]: Failed to delete relationship in Neo4j for user: %s", username);
        }
    }

    /**
     * Handles the following of a user in Neo4j.
     *
     * @param username       the nodeId of the user who followed
     * @param targetUsername the nodeId of the user being followed
     */
    public void userFollow(String username, String targetUsername) {
        Node userNode = new Node(Node.NodeType.USER, username);
        Node targetUserNode = new Node(Node.NodeType.USER, targetUsername);
        Relationship relationship = new Relationship(userNode, targetUserNode, Relationship.RelationshipType.FOLLOWS, System.currentTimeMillis());
        neo4jRepo.createOrUpdateRelationship(relationship);
        logger.info("[USER FOLLOW][SERVICE]: Creating relationship: " + relationship);
        UserActionEvent event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.USER_FOLLOWED);
        event.setUsername(username);
        event.setTargetUsername(targetUsername);
        logger.info("[USER FOLLOW][SERVICE]: Publishing event: " + event);
        publisher.publishAction(event, UserActionEvent.ActionType.USER_FOLLOWED.getValue());
    }

    /**
     * Handles the unfollowing of a user in Neo4j.
     *
     * @param username       the nodeId of the user who unfollowed
     * @param targetUsername the nodeId of the user being unfollowed
     */
    public void userUnfollow(String username, String targetUsername) {
        Node userNode = new Node(Node.NodeType.USER, username);
        Node targetUserNode = new Node(Node.NodeType.USER, targetUsername);
        Relationship relationship = new Relationship(userNode, targetUserNode, Relationship.RelationshipType.FOLLOWS, System.currentTimeMillis());
        logger.info("[USER UNFOLLOW][SERVICE]: Deleting relationship: " + relationship);
        boolean success = neo4jRepo.deleteRelationship(relationship);
        if (!success) {
            logger.errorf("[CREATE USER][SERVICE]: Failed to delete relationship in Neo4j for user: %s", username);
        }
        UserActionEvent event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.USER_UNFOLLOWED);
        event.setUsername(username);
        event.setTargetUsername(targetUsername);
        logger.info("[USER UNFOLLOW][SERVICE]: Publishing event: " + event);
        publisher.publishAction(event, UserActionEvent.ActionType.USER_UNFOLLOWED.getValue());
    }

    /**
     * Handles the liking of a post in Neo4j.
     *
     * @param username the nodeId of the user who liked the post
     * @param postId   the UUID of the post
     */
    public void postLike(String username, String postId) {
        Node userNode = new Node(Node.NodeType.USER, username);
        Node postNode = new Node(Node.NodeType.POST, postId);
        Relationship relationship = new Relationship(userNode, postNode, Relationship.RelationshipType.LIKES, System.currentTimeMillis());
        logger.info("[POST LIKE][SERVICE]: Creating relationship: " + relationship);
        neo4jRepo.createOrUpdateRelationship(relationship);

        UserActionEvent event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.POST_LIKED);
        event.setUsername(username);
        event.setPostId(postId);
        logger.info("[POST LIKE][SERVICE]: Publishing event: " + event);
        publisher.publishAction(event, UserActionEvent.ActionType.POST_LIKED.getValue());
    }

    /**
     * Handles the unliking of a post in Neo4j.
     *
     * @param postId   the UUID of the post
     * @param username the nodeId of the user who unliked the post
     */
    public void postUnlike(String postId, String username) {
        neo4jRepo.deleteLikeRelationship(username, postId);
        UserActionEvent event = new UserActionEvent();
        event.setActionType(UserActionEvent.ActionType.POST_UNLIKED);
        event.setUsername(username);
        event.setPostId(postId);
        logger.info("[POST UNLIKE][SERVICE]: Publishing event: " + event);
        publisher.publishAction(event, UserActionEvent.ActionType.POST_UNLIKED.getValue());
    }

    /**
     * Vérifie si un utilisateur a déjà aimé un post spécifique.
     *
     * @param username le nom d'utilisateur
     * @param postId l'identifiant du post
     * @return true si l'utilisateur a aimé le post, false sinon
     */
    public boolean hasUserLikedPost(String username, String postId) {
        // Implémentation qui vérifie l'existence d'une relation LIKES entre l'utilisateur et le post
        boolean hasLiked = neo4jRepo.likeRelationshipExists(postId, username);
        if (hasLiked) {
            logger.info("[USER LIKED POST][SERVICE]: User " + username + " has liked post " + postId);
        } else {
            logger.info("[USER LIKED POST][SERVICE]: User " + username + " has not liked post " + postId);
        }
        return hasLiked;
    }

    /**
     * Vérifie si un post existe dans Neo4j.
     *
     * @param postId l'identifiant du post
     * @return true si le post existe, false sinon
     */
    public boolean postExists(String postId) {
        boolean exists = neo4jRepo.nodeExists(postId, Node.NodeType.POST);
        if (exists) {
            logger.info("[POST EXISTS][SERVICE]: Post " + postId + " exists.");
        } else {
            logger.info("[POST EXISTS][SERVICE]: Post " + postId + " does not exist.");
        }
        return exists;
    }

    /**
     * Checks if a user exists in Neo4j.
     *
     * @param username the nodeId to check
     * @return true if the user exists, false otherwise
     */
    public boolean userExists(String username) {
        boolean exists = neo4jRepo.nodeExists(username, Node.NodeType.USER);
        if (exists) {
            logger.info("[USER EXISTS][SERVICE]: User " + username + " exists.");
        } else {
            logger.info("[USER EXISTS][SERVICE]: User " + username + " does not exist.");
        }
        return exists;
    }

    /**
     * Checks if a user is blocked by another user.
     *
     * @param username       the nodeId of the user
     * @param targetUsername the nodeId of the target user
     * @return true if the user is blocked, false otherwise
     */
    public boolean isUserBlocked(String username, String targetUsername) {
        boolean isBlocked = neo4jRepo.userRelationshipExists(username, targetUsername, "BLOCKS");
        if (isBlocked) {
            logger.info("[USER BLOCKED][SERVICE]: User " + username + " is blocked by " + targetUsername);
        } else {
            logger.info("[USER BLOCKED][SERVICE]: User " + username + " is not blocked by " + targetUsername);
        }
        return isBlocked;
    }

    /**
     * Checks if a user is following another user.
     *
     * @param username       the nodeId of the user
     * @param targetUsername the nodeId of the target user
     * @return true if the user is following, false otherwise
     */
    public boolean isUserFollowing(String username, String targetUsername) {
        boolean isFollowing = neo4jRepo.userRelationshipExists(username, targetUsername, "FOLLOWS");
        if (isFollowing) {
            logger.info("[USER FOLLOWING][SERVICE]: User " + username + " is following " + targetUsername);
        } else {
            logger.info("[USER FOLLOWING][SERVICE]: User " + username + " is not following " + targetUsername);
        }
        return isFollowing;
    }

    /**
     * Retrieves the author of a post.
     *
     * @param postId the UUID of the post
     * @return the nodeId of the author
     */
    public String getPostAuthor(String postId) {
        Node postNode = new Node(Node.NodeType.POST, postId);
        String author = neo4jRepo.getPostAuthor(postNode);
        if (author != null) {
            logger.info("[POST AUTHOR][SERVICE]: Author of post " + postId + " is " + author);
        } else {
            logger.info("[POST AUTHOR][SERVICE]: No author found for post " + postId);
        }
        return author;
    }

    /**
     * Retrieves the list of users who liked a post.
     *
     * @param postId the UUID of the post
     * @return a list of usernames who liked the post
     */
    public List<String> getPostLikeUsers(String postId) {
        List<String> users = neo4jRepo.getPostLikeUsers(postId);
        if (users != null && !users.isEmpty()) {
            logger.info("[POST LIKED USERS][SERVICE]: Users who liked post " + postId + ": " + users);
        } else {
            logger.info("[POST LIKED USERS][SERVICE]: No users found who liked post " + postId);
        }
        return users;
    }

    /**
     * Retrieves the list of posts liked by a user.
     *
     * @param userId the nodeId of the user
     * @return a list of post IDs liked by the user
     */
    public List<String> getUserLikedPosts(String userId) {;
        List<String> posts = neo4jRepo.getUserLikedPosts(userId);
        if (posts != null && !posts.isEmpty()) {
            logger.info("[USER LIKED POSTS][SERVICE]: Posts liked by user " + userId + ": " + posts);
        } else {
            logger.info("[USER LIKED POSTS][SERVICE]: No posts found liked by user " + userId);
        }
        return posts;
    }

    /**
     * Retrieves the list of followers of a user.
     *
     * @param userId the nodeId of the user
     * @return a list of usernames who follow the user
     */
    public List<String> getUserFollowers(String userId) {
        List<String> followers = neo4jRepo.getUserFollowers(userId);
        if (followers != null && !followers.isEmpty()) {
            logger.info("[USER FOLLOWERS][SERVICE]: Followers of user " + userId + ": " + followers);
        } else {
            logger.info("[USER FOLLOWERS][SERVICE]: No followers found for user " + userId);
        }
        return followers;
    }

    /**
     * Retrieves the list of users followed by a user.
     *
     * @param userId the nodeId of the user
     * @return a list of usernames followed by the user
     */
    public List<String> getUserFollows(String userId) {
        List<String> follows = neo4jRepo.getUserFollows(userId);
        if (follows != null && !follows.isEmpty()) {
            logger.info("[USER FOLLOWS][SERVICE]: Users followed by user " + userId + ": " + follows);
        } else {
            logger.info("[USER FOLLOWS][SERVICE]: No users found followed by user " + userId);
        }
        return follows;
    }

    /**
     * Retrieves the list of users blocked by a user.
     *
     * @param userId the nodeId of the user
     * @return a list of usernames blocked by the user
     */
    public List<String> getUserBlockedUsers(String userId) {
        List<String> blockedUsers = neo4jRepo.getUserBlockedUsers(userId);
        if (blockedUsers != null && !blockedUsers.isEmpty()) {
            logger.info("[USER BLOCKED USERS][SERVICE]: Blocked users by " + userId + ": " + blockedUsers);
        } else {
            logger.info("[USER BLOCKED USERS][SERVICE]: No users found blocked by " + userId);
        }
        return blockedUsers;
    }

    /**
     * Retrieves the list of users blocking a user.
     *
     * @param userId the nodeId of the user
     * @return a list of usernames who block the user
     */
    public List<String> getUserBlockingUsers(String userId) {
        List<String> blockingUsers = neo4jRepo.getUserBlockingUsers(userId);
        if (blockingUsers != null && !blockingUsers.isEmpty()) {
            logger.info("[USER BLOCKING USERS][SERVICE]: Users blocking " + userId + ": " + blockingUsers);
        } else {
            logger.info("[USER BLOCKING USERS][SERVICE]: No users found blocking " + userId);
        }
        return blockingUsers;
    }
}
