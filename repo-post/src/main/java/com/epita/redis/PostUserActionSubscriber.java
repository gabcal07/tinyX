package com.epita.redis;

import com.epita.events.UserActionEvent;
import com.epita.repository.PostRepoMongo;
import com.epita.repository.RegisteredUsersRepo;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import static io.quarkus.mongodb.runtime.dns.MongoDnsClientProvider.vertx;

/**
 * Subscriber for user action events from Redis.
 */
@Startup
@ApplicationScoped
public class PostUserActionSubscriber implements Consumer<UserActionEvent> {

    @Inject Logger logger;

    @Inject
    RegisteredUsersRepo registeredUsersRepo;

    @Inject
    PostRepoMongo postRepoMongo;

    private final List<PubSubCommands.RedisSubscriber> subscribers = new ArrayList<>();
    /**
     * Constructor to initialize the Redis subscriber.
     *
     * @param ds the Redis data source
     */
    public PostUserActionSubscriber(RedisDataSource ds) {
        subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.USER_DELETED.getValue(), this));
        subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.USER_CREATED.getValue(), this));
        subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.USER_BLOCKED.getValue(), this));
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
            case USER_BLOCKED:
                handleAddBlockedUser(event.getUsername(), event.getTargetUsername());
                break;
            case USER_CREATED:
                handleReferencingRegisteredUser(event.getUsername());
                break;
            case USER_DELETED:
                handleUserPostDeletion(event.getUsername());
                handleUserDeletion(event.getUsername());
            default:
                logger.warn("Unhandled event type: " + event.getActionType());
        }
    }

    /**
     * Handles the deletion of a user.
     *
     * @param username the ID of the user to delete
     */
    private void handleUserDeletion(String username) {
        registeredUsersRepo.deleteUserWithUsername(username);
    }

    /**
     * Handles the addition of a blocked user.
     *
     * @param username        the ID of the user who blocked
     * @param blockedUsername the ID of the blocked user
     */
    private void handleAddBlockedUser(String username, String blockedUsername) {
        registeredUsersRepo.addBlockedUser(username, blockedUsername);
    }

    /**
     * Handles the registration of a new user.
     *
     * @param username the ID of the registered user
     */
    private void handleReferencingRegisteredUser(String username) {
        registeredUsersRepo.registerUser(username);
    }

    /**
     * Handles the deletion of user posts.
     *
     * @param username the ID of the user whose posts are to be deleted
     */
    private void handleUserPostDeletion(String username) {
        postRepoMongo.deleteUsersPosts(username);
    }

    /**
     * Unsubscribes from the Redis channel on termination.
     */
    @PreDestroy
    public void terminate() {
        subscribers.forEach(PubSubCommands.RedisSubscriber::unsubscribe);
    }
}