package com.epita.redis;

import com.epita.events.UserActionEvent;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DeletePostPublisherTest {
    private final PubSubCommands<UserActionEvent> publisher;

    public DeletePostPublisherTest(final RedisDataSource ds) {
        publisher = ds.pubsub(UserActionEvent.class);
    }

    public void publish(final UserActionEvent message) {
        publisher.publish(UserActionEvent.ActionType.POST_DELETED.getValue(), message);
    }
}
