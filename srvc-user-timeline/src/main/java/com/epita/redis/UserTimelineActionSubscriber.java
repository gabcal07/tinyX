package com.epita.redis;

import com.epita.events.UserActionEvent;
import java.util.Date;
import com.epita.repository.ServiceUserTimelineMongoRepo;
import com.epita.repository.models.UserTimelineModel;
import com.epita.service.entities.PostReferenceEntity;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import static io.quarkus.mongodb.runtime.dns.MongoDnsClientProvider.vertx;

/**
 * Subscriber for user action events from Redis.
 */
@Startup
@ApplicationScoped
public class UserTimelineActionSubscriber implements Consumer<UserActionEvent> {

    @Inject Logger logger;

    @Inject
    ServiceUserTimelineMongoRepo userTimelineRepo;

    private final List<PubSubCommands.RedisSubscriber> subscribers = new ArrayList<>();
    /**
     * Constructor to initialize the Redis subscriber.
     *
     * @param ds the Redis data source
     */
    public UserTimelineActionSubscriber(RedisDataSource ds) {
        subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.POST_CREATED.getValue(), this));
        subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.POST_DELETED.getValue(), this));
        subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.POST_LIKED.getValue(), this));
        subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.POST_UNLIKED.getValue(), this));
        subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.USER_CREATED.getValue(), this));
        subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.USER_DELETED.getValue(), this));
    }

    /**
     * Handles incoming user action events.
     *
     * @param event the user action event
     */
    @Override
    public void accept(UserActionEvent event) {
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
     * Processes the user action event based on its type.
     *
     * @param event the user action event
     */
    private void handleEvent(UserActionEvent event) {
        switch (event.getActionType())
        {
            case USER_CREATED:
                handleTimelineCreation(event.getUsername(), event.getUserId());
                break;
            case USER_DELETED:
                handleTimelineDeletion(event.getUsername());
                break;
            case POST_CREATED:
                handleTimelineAddition(event.getUsername(), event.getPostId(), "AUTHORED", event.getTimestamp());
                break;
            case POST_LIKED:
                handleTimelineAddition(event.getUsername(), event.getPostId(), "LIKED", event.getTimestamp());
                break;
            case POST_DELETED:
            case POST_UNLIKED:
                handlePostDeletion(event.getUsername(), event.getPostId());
                break;
            default:
                logger.warn("Unhandled event type: " + event.getActionType());
        }
    }

    /**
     * Handles the addition of a post.
     *
     * @param username        the ID of the user
     * @param postId the ID of the post
     * @param type type of action
     * @param date timestamp of the action
     **/
    private void handleTimelineAddition(String username, String postId, String type, Date date) {
        UserTimelineModel timeline = userTimelineRepo.find("_id", username).firstResult();
        List<PostReferenceEntity> posts = timeline.getPosts();
        PostReferenceEntity post = new PostReferenceEntity(postId, type, date);
        posts.add(post);
        userTimelineRepo.updateUserTimeLineWithUsername(username, posts);
    }


    /**
     * Handles the deletion of a post.
     *
     * @param username        the ID of the user
     * @param postId the ID of the post
     **/
    private void handlePostDeletion(String username, String postId){
        UserTimelineModel timeline = userTimelineRepo.getUserTimelineWithUsername(username);
        List<PostReferenceEntity> posts = timeline.getPosts();
        posts.removeIf(json -> json.getPostId().equals(postId));
        userTimelineRepo.updateUserTimeLineWithUsername(username, posts);
    }


    /**
     * Handles the creation of a userTimeline
     *
     * @param username the username of the user
     * @param userId the id of the user
     */

    public void handleTimelineCreation(String username, String userId)
    {
        userTimelineRepo.createUserTimeline(username, userId, new ArrayList<>());
    }

    /**
     * Handles the deletion of a userTimeline
     *
     * @param username the username of the user
     */

    private void handleTimelineDeletion(String username)
    {
        userTimelineRepo.deleteUserTmelineWithUsername(username);
    }

    /**
     * Unsubscribes from the Redis channel on termination.
     */
    @PreDestroy
    public void terminate() {
        subscribers.forEach(PubSubCommands.RedisSubscriber::unsubscribe);
    }
}
