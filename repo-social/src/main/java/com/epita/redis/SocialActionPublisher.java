package com.epita.redis;

import com.epita.events.UserActionEvent;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Publisher for user action events to Redis.
 */
@ApplicationScoped
public class SocialActionPublisher {
    private final PubSubCommands<UserActionEvent> publisher;

    /**
     * Constructor to initialize the Redis publisher.
     *
     * @param ds the Redis data source
     */
    public SocialActionPublisher(RedisDataSource ds) {
        publisher = ds.pubsub(UserActionEvent.class);
    }

    /**
     * Publishes a user action event to the Redis channel.
     *
     * @param event the user action event to publish
     * @param pubChannel the channel to publish the event to
     */
    public void publishAction(UserActionEvent event, String pubChannel) {
        publisher.publish(pubChannel, event);
    }
}
