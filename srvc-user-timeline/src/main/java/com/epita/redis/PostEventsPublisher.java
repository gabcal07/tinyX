package com.epita.redis;

import com.epita.events.UserActionEvent;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PostEventsPublisher {
    private final PubSubCommands<UserActionEvent> publisher;

    public PostEventsPublisher(final RedisDataSource ds) {
        publisher = ds.pubsub(UserActionEvent.class);
    }

    public void publishPostCreated(final UserActionEvent event) {
        publisher.publish(UserActionEvent.ActionType.POST_CREATED.getValue(), event);
    }

    public void publishPostDeleted(final UserActionEvent event) {
        publisher.publish(UserActionEvent.ActionType.POST_DELETED.getValue(), event);
    }

    public void publishPostLiked(final UserActionEvent event) {
        publisher.publish(UserActionEvent.ActionType.POST_LIKED.getValue(), event);
    }

    public void publishPostUnliked(final UserActionEvent event) {
        publisher.publish(UserActionEvent.ActionType.POST_UNLIKED.getValue(), event);
    }
}
