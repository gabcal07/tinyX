package com.epita.redis;

import com.epita.events.UserActionEvent;
import com.epita.service.HomeTimelineService;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.jboss.logging.Logger;

import static io.quarkus.mongodb.runtime.dns.MongoDnsClientProvider.vertx;

@Startup
@ApplicationScoped
public class HomeTimelineSubscriber implements Consumer<UserActionEvent> {

    @Inject
    HomeTimelineService hometimelineservice;

    private final List<PubSubCommands.RedisSubscriber> subscribers = new ArrayList<>();

    @Inject
    Logger logger;

    public HomeTimelineSubscriber(RedisDataSource ds) {
        System.out.println("Subscribing to Redis channel...");
        this.subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.POST_CREATED.getValue(), this));
        this.subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.POST_DELETED.getValue(), this));
        this.subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.POST_UNLIKED.getValue(), this));
        this.subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.POST_LIKED.getValue(), this));
        this.subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.USER_FOLLOWED.getValue(), this));
        this.subscribers.add(ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.USER_UNFOLLOWED.getValue(), this));
        System.out.println("Subscribed successfully.");
    }

    @Override
    public void accept(UserActionEvent event) {
        logger.infof("[%s][SUBSCRIBER]: Received event: %s", event.getActionType(), event);
        vertx.executeBlocking(future -> {
            try
            {
                handleEvent(event);
                future.complete();
            } catch (Exception e) {
                logger.errorf("[%s][SUBSCRIBER]: Error processing event: %s, error: %s", event.getActionType(), event, e.getMessage());
                future.fail(e);
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
            case POST_CREATED -> {
                hometimelineservice.addPost(event.getUsername(), event.getPostId(), event.getTimestamp());
                logger.infof("[POST CREATED][SERVICE]:User created: %s", event.getUsername());
            }
            case POST_DELETED -> {
                hometimelineservice.removePost(event.getUsername(), event.getPostId());
                logger.infof("[POST DELETED][SERVICE]:User deleted: %s", event.getUserId());
            }
            case POST_UNLIKED -> {
                hometimelineservice.removeLike(event.getUsername(), event.getPostId());
                logger.infof("[POST UNLIKED][SERVICE]:Post deleted: %s", event.getPostId());
            }
            case POST_LIKED -> {
                hometimelineservice.addLike(event.getUsername(), event.getPostId());
                logger.infof("[POST LIKED][SERVICE]:Post created: %s", event.getPostId());
            }
            case USER_FOLLOWED -> {
                hometimelineservice.addFollow(event.getUsername(), event.getTargetUsername());
                logger.infof("[USER FOLLOW][SERVICE]:User followed: %s", event.getTargetUsername());
            }
            case USER_UNFOLLOWED -> {
                hometimelineservice.removeFollow(event.getUsername(), event.getTargetUsername());
                logger.infof("[USER UNFOLLOW][SERVICE]:User unfollowed: %s", event.getTargetUsername());
            }
            default -> logger.warn("Unhandled event type: " + event.getActionType());
        }
    }

    @PreDestroy
    public void terminate() {
        subscribers.forEach(PubSubCommands.RedisSubscriber::unsubscribe);
        logger.info("[DELETE POST][SUBSCRIBER]: Unsubscribed from CREATE_POST events.");
    }
}
