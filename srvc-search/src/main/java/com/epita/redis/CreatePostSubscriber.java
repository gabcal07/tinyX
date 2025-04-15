package com.epita.redis;

import com.epita.events.UserActionEvent;
import com.epita.service.SearchService;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.function.Consumer;

import static io.quarkus.mongodb.runtime.dns.MongoDnsClientProvider.vertx;

@Startup
@ApplicationScoped
public class CreatePostSubscriber implements Consumer<UserActionEvent> {

    @Inject
    SearchService searchService;
    private final PubSubCommands.RedisSubscriber subscriber;


    public CreatePostSubscriber(RedisDataSource ds) {
        this.subscriber = ds.pubsub(UserActionEvent.class).subscribe(UserActionEvent.ActionType.POST_CREATED.getValue(), this);
    }

    @Override
    public void accept(final UserActionEvent actionEvent) {
        vertx.executeBlocking(future -> {
            try {
                searchService.createPost(actionEvent);
                future.complete();
            } catch (Exception e) {
                future.fail(e);
            }

        });
    }

    @PreDestroy
    public void terminate() {
        subscriber.unsubscribe();
    }
}
