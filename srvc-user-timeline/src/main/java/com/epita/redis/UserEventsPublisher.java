package com.epita.redis;

import com.epita.events.UserActionEvent;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserEventsPublisher {
    private final PubSubCommands<UserActionEvent> publisher;

    public UserEventsPublisher(final RedisDataSource ds) {
        publisher = ds.pubsub(UserActionEvent.class);
    }

    public void publishUserCreated(final UserActionEvent event) {
        publisher.publish(UserActionEvent.ActionType.USER_CREATED.getValue(), event);
    }

    public void publishUserDeleted(final UserActionEvent event) {
        publisher.publish(UserActionEvent.ActionType.USER_DELETED.getValue(), event);
    }
}
