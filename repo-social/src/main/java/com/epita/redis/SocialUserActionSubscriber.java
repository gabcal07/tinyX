package com.epita.redis;

import com.epita.events.UserActionEvent;
import com.epita.repository.SocialRepoNeo4j;
import com.epita.repository.models.Node;
import com.epita.repository.models.Relationship;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import static io.quarkus.mongodb.runtime.dns.MongoDnsClientProvider.vertx;

/**
 * Subscriber for post action events from Redis.
 */
@Startup
@ApplicationScoped
public class SocialUserActionSubscriber implements Consumer<UserActionEvent> {

    @Inject Logger logger;

    @Inject
    SocialRepoNeo4j neo4jRepo;

    private final List<PubSubCommands.RedisSubscriber> subscribers = new ArrayList<>();


    /**
     * Constructor to initialize the Redis subscriber.
     *
     * @param ds the Redis data source
     */
    public SocialUserActionSubscriber(RedisDataSource ds) {
        subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.USER_DELETED.getValue(), this));
        subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.USER_CREATED.getValue(), this));
        subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.POST_DELETED.getValue(), this));
        subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.POST_CREATED.getValue(), this));
    }

    /**
     * Handles incoming post action events.
     *
     * @param event the post action event
     */
    @Override
    public void accept(UserActionEvent event) {
        logger.infof("[Event Start] %s", event);
        vertx.executeBlocking(future -> {
            // Traitement synchrone ici
            logger.infof("[Event Start] %s", event);
            handleEvent(event);
            logger.infof("[Event Success] %s", event);
            future.complete();
        }, res -> {
            if (res.failed()) {
                logger.errorf("[Event Failed] %s || Error: %s", event, res.cause().getMessage());
            }
        });
    }

    /**
     * Processes the post action event based on its type.
     *
     * @param event the post action event
     */
    private void handleEvent(UserActionEvent event) {
        switch (event.getActionType())
        {
            case USER_CREATED -> {
                handleUserCreation(event.getUsername());
                logger.infof("[USER CREATE][SERVICE]:User created: %s", event.getUsername());
            }
            case USER_DELETED -> {
                handleUserDeletion(event.getUsername());
                logger.infof("[USER DELETE][SERVICE]:User deleted: %s", event.getUserId());
            }
            case POST_DELETED -> {
                handlePostAndRelationDeletion(event.getPostId());
                logger.infof("[POST DELETE][SERVICE]:Post deleted: %s", event.getPostId());
            }
            case POST_CREATED -> {
                handlePostCreation(event.getPostId(), event.getUsername());
                logger.infof("[POST CREATED][SERVICE]:Post created: %s", event.getPostId());
            }
            default -> logger.warn("Unhandled event type: " + event.getActionType());
        }
    }

    /**
     * Handles the creation of a user in Neo4j.
     *
     * @param username the nodeId of the user
     */
    private void handleUserCreation(String username) {
        boolean success = neo4jRepo.createUser(username);
        if (!success) {
            logger.errorf("[USER CREATION][REDIS]: Failed to create user node in Neo4j for nodeId: %s", username);
        }
    }

    /**
     * Handles the creation of a post in Neo4j.
     *
     * @param postId the UUID of the post
     */
    private void handlePostCreation(String postId, String username) {
        logger.info("[POST CREATE][REDIS]: Creating post: " + postId);
        Boolean success = neo4jRepo.createPostNode(postId);

        // Create the relationship between the user and the post
        if (success) {
            Node userNode = new Node(Node.NodeType.USER, username);
            Node postNode = new Node(Node.NodeType.POST, postId);
            Relationship relationship = new Relationship(userNode, postNode, Relationship.RelationshipType.POSTED, System.currentTimeMillis());
            neo4jRepo.createOrUpdateRelationship(relationship);
            logger.info("[POST CREATE][REDIS]: Creating relationship: " + username + "posted post with " + postId);
        }
        if (!success) {
            logger.errorf("[POST CREATE][SERVICE]:Failed to create post node in Neo4j for postId: %s", postId);
        }
    }

    /**
     * Handles the deletion of a user in Neo4j.
     * Deletes its posts and relationships.
     * @param username the nodeId of the user
     */
    private void handleUserDeletion(String username) {
        handleUserRelationsDeletion(username);
        boolean success = neo4jRepo.deleteUserWithUserId(username);
        if (!success) {
            logger.errorf("[USER DELETE][REDIS]:Failed to delete user node in Neo4j for nodeId: %s", username);
        }
        neo4jRepo.deleteUsersPosts(username);
    }

    /**
     * Handles the deletion of post relationships in Neo4j.
     *
     * @param postId the UUID of the post
     */
    private void handlePostAndRelationDeletion(String postId) {
        neo4jRepo.deleteRelationshipsForPost(UUID.fromString(postId));
        neo4jRepo.deletePostWithPostId(postId);
        logger.info("[POST DELETE RELATIONS][REDIS]: Deleted relationship: " + postId);
    }

    /**
     * Handles the deletion of user relationships in Neo4j.
     *
     * @param username the nodeId of the user
     */
    private void handleUserRelationsDeletion(String username) {
        neo4jRepo.deleteRelationshipsForUser(username);
        logger.info("[USER DELETE RELATIONS][REDIS]: Deleting relationship: " + username);
    }

    /**
     * Unsubscribes from the Redis channel on termination.
     */
    @PreDestroy
    public void terminate() {
        for (PubSubCommands.RedisSubscriber subscriber : subscribers) {
            subscriber.unsubscribe();
        }
        logger.info("[Redis Subscriber] Unsubscribed from all channels.");
    }
}